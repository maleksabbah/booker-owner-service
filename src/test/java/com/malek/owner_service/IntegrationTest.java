package com.malek.owner_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static final String JWT_SECRET = "dev-shared-secret-please-change-at-least-32-characters-long";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booker_test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("spring.liquibase.change-log", () ->
                "classpath:db/changelog/db.changelog-master.yaml");
        registry.add("spring.liquibase.enabled",    () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired TestRestTemplate http;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;

    static Long ownerUserId;
    static Long staffUserId;
    static Long facilityId;
    static Long courtId;
    static Long bookingId;
    static String inviteToken;

    @BeforeAll
    void seedUsers() {
        ownerUserId = insertUser("owner@test.com", "Owner User");
        staffUserId = insertUser("staff@test.com", "Staff User");
        grantRole(ownerUserId, "OWNER");
        grantRole(staffUserId, "OWNER");
    }

    @Test @Order(1)
    void createFacility() {
        Map<String, Object> body = Map.of(
                "name",    "Test Soho Pitches",
                "address", "1 Test Lane",
                "city",    "London"
        );
        ResponseEntity<JsonNode> resp = post("/owner/facilities", body, ownerUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        facilityId = resp.getBody().get("id").asLong();
        assertThat(resp.getBody().get("ownerUserId").asLong()).isEqualTo(ownerUserId);
        assertThat(resp.getBody().get("active").asBoolean()).isTrue();
    }

    @Test @Order(2)
    void listMyFacilitiesContainsTheNewOne() {
        ResponseEntity<JsonNode> resp = get("/owner/facilities", ownerUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
        boolean found = false;
        for (JsonNode n : resp.getBody()) {
            if (n.get("id").asLong() == facilityId) { found = true; break; }
        }
        assertThat(found).isTrue();
    }

    @Test @Order(3)
    void addCourt() {
        Map<String, Object> body = Map.of(
                "name",         "Court A",
                "sport",        "FOOTBALL_5",
                "surface",      "TURF",
                "capacity",     10,
                "pricePerHour", 30,
                "currency",     "USD"
        );
        ResponseEntity<JsonNode> resp = post("/owner/facilities/" + facilityId + "/courts", body, ownerUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        courtId = resp.getBody().get("id").asLong();
        assertThat(resp.getBody().get("facilityId").asLong()).isEqualTo(facilityId);
    }

    @Test @Order(4)
    void setWeeklyAvailability() {
        Map<String, Object> body = Map.of(
                "slots", List.of(
                        Map.of("dayOfWeek", 1, "opensAt", "09:00:00", "closesAt", "22:00:00"),
                        Map.of("dayOfWeek", 2, "opensAt", "09:00:00", "closesAt", "22:00:00"),
                        Map.of("dayOfWeek", 3, "opensAt", "09:00:00", "closesAt", "22:00:00")
                )
        );
        ResponseEntity<JsonNode> resp = put("/owner/courts/" + courtId + "/availability", body, ownerUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().size()).isEqualTo(3);
    }

    @Test @Order(5)
    void addBlackout() {
        Map<String, Object> body = Map.of(
                "startsAt", "2099-12-25T00:00:00",
                "endsAt",   "2099-12-25T23:59:59",
                "reason",   "Christmas Day"
        );
        ResponseEntity<JsonNode> resp = post("/owner/courts/" + courtId + "/blackouts", body, ownerUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("reason").asText()).isEqualTo("Christmas Day");
    }

    @Test @Order(6)
    void inviteStaffMember() {
        Map<String, Object> body = Map.of("email", "staff@test.com", "role", "STAFF");
        ResponseEntity<JsonNode> resp = post("/owner/facilities/" + facilityId + "/invitations", body, ownerUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        inviteToken = resp.getBody().get("token").asText();
        assertThat(inviteToken).hasSize(32);
        assertThat(resp.getBody().get("status").asText()).isEqualTo("PENDING");
    }

    @Test @Order(7)
    void staffNotYetMember() {
        ResponseEntity<JsonNode> resp = get("/owner/facilities/" + facilityId, staffUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(8)
    void simulateInviteAcceptance() {
        jdbc.update("""
            INSERT INTO facility_members (facility_id, user_id, role, added_by_user_id, added_at)
            VALUES (?, ?, 'STAFF', ?, NOW())
            """, facilityId, staffUserId, ownerUserId);
        jdbc.update("UPDATE facility_invitations SET accepted_at = NOW() WHERE token = ?", inviteToken);
    }

    @Test @Order(9)
    void staffCanNowViewFacility() {
        ResponseEntity<JsonNode> resp = get("/owner/facilities/" + facilityId, staffUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @Order(10)
    void staffCannotInviteOthers() {
        Map<String, Object> body = Map.of("email", "rogue@test.com", "role", "STAFF");
        ResponseEntity<JsonNode> resp = post("/owner/facilities/" + facilityId + "/invitations", body, staffUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(11)
    void simulateBookingCreation() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end   = start.plusHours(1);
        jdbc.update("""
            INSERT INTO bookings (court_id, creator_user_id, slot_start, slot_end,
                                  visibility, state, slots_total, slots_filled, min_players,
                                  created_at, updated_at)
            VALUES (?, ?, ?, ?, 'PRIVATE', 'CONFIRMED', 10, 10, 1, NOW(), NOW())
            """, courtId, ownerUserId, start, end);
        bookingId = jdbc.queryForObject(
                "SELECT id FROM bookings WHERE court_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, courtId);
        assertThat(bookingId).isNotNull();
    }

    @Test @Order(12)
    void staffSeesScheduleIncludesBooking() {
        ResponseEntity<JsonNode> resp = get(
                "/owner/facilities/" + facilityId + "/bookings", staffUserId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
        boolean found = false;
        for (JsonNode n : resp.getBody()) {
            if (n.get("id").asLong() == bookingId) { found = true; break; }
        }
        assertThat(found).isTrue();
    }

    @Test @Order(13)
    void staffMarksSeated() {
        ResponseEntity<JsonNode> resp = postWithIdempotency(
                "/owner/bookings/" + bookingId + "/seat", null, staffUserId, UUID.randomUUID().toString());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("state").asText()).isEqualTo("SEATED");
        assertThat(resp.getBody().get("seatedByUserId").asLong()).isEqualTo(staffUserId);
    }

    @Test @Order(14)
    void cannotSeatTwice() {
        ResponseEntity<JsonNode> resp = postWithIdempotency(
                "/owner/bookings/" + bookingId + "/seat", null, staffUserId, UUID.randomUUID().toString());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test @Order(15)
    void idempotencyReplayReturnsSameResponse() {
        String key = UUID.randomUUID().toString();
        ResponseEntity<JsonNode> first = postWithIdempotency(
                "/owner/bookings/" + bookingId + "/complete", null, staffUserId, key);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().get("state").asText()).isEqualTo("COMPLETED");

        ResponseEntity<JsonNode> second = postWithIdempotency(
                "/owner/bookings/" + bookingId + "/complete", null, staffUserId, key);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody().get("id").asLong()).isEqualTo(first.getBody().get("id").asLong());
    }

    // ─── helpers ────────────────────────────────────────────────────

    private Long insertUser(String email, String displayName) {
        jdbcTemplate.update("""
        INSERT INTO users (email, username, password_hash, name, active, created_at, updated_at)
        VALUES (?, ?, 'unused', ?, true, NOW(), NOW())
        """, email, email.split("@")[0] + "_" + System.nanoTime(), displayName);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private void grantRole(Long userId, String role) {
        jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, role);
    }

    private String mintJwt(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", List.of("OWNER"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key)
                .compact();
    }

    private HttpHeaders authHeaders(Long userId) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(mintJwt(userId));
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private ResponseEntity<JsonNode> get(String path, Long userId) {
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(authHeaders(userId)), JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(String path, Object body, Long userId) {
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, authHeaders(userId)), JsonNode.class);
    }

    private ResponseEntity<JsonNode> put(String path, Object body, Long userId) {
        return http.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, authHeaders(userId)), JsonNode.class);
    }

    private ResponseEntity<JsonNode> postWithIdempotency(String path, Object body, Long userId, String key) {
        HttpHeaders h = authHeaders(userId);
        h.add("Idempotency-Key", key);
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), JsonNode.class);
    }
}
