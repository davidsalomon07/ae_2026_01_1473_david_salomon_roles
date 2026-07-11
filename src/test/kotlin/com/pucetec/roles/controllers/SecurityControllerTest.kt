package com.pucetec.roles.controllers

import com.pucetec.roles.dto.CreateParkingSpaceRequest
import com.pucetec.roles.dto.EntryRequest
import com.pucetec.roles.dto.ExitRequest
import com.pucetec.roles.dto.ParkingSpaceResponse
import com.pucetec.roles.dto.TicketResponse
import com.pucetec.roles.services.ParkingSpaceService
import com.pucetec.roles.services.TicketService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [ParkingSpaceController::class, TicketController::class])
class SecurityControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var parkingSpaceService: ParkingSpaceService

    @MockitoBean
    private lateinit var ticketService: TicketService

    // ───────────────────────────── /parking-spaces/available (Público) ─────────────────────────────

    @Test
    fun `GET available devuelve 200 sin necesidad de autenticacion`() {
        `when`(parkingSpaceService.getAvailableSpaces()).thenReturn(emptyList())

        mockMvc.perform(
            get("/parking-spaces/available")
        ).andExpect(status().isOk)
    }

    // ───────────────────────────── POST /parking-spaces (ADMIN) ─────────────────────────────

    @Test
    fun `POST parking-spaces sin token devuelve 401 Unauthorized`() {
        mockMvc.perform(
            post("/parking-spaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"A10"}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST parking-spaces con rol USER devuelve 403 Forbidden`() {
        mockMvc.perform(
            post("/parking-spaces")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"A10"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST parking-spaces con rol ADMIN devuelve 201 Created`() {
        val request = CreateParkingSpaceRequest(code = "A10")
        val response = ParkingSpaceResponse(id = 10L, code = "A10", occupied = false)
        `when`(parkingSpaceService.createSpace(request)).thenReturn(response)

        mockMvc.perform(
            post("/parking-spaces")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"A10"}""")
        ).andExpect(status().isCreated)
    }

    // ───────────────────────────── POST /tickets/entry (USER) ─────────────────────────────

    @Test
    fun `POST entry sin token devuelve 401 Unauthorized`() {
        mockMvc.perform(
            post("/tickets/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"plate":"ABC-1234","parkingSpaceId":1}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST entry con rol ADMIN devuelve 403 Forbidden`() {
        mockMvc.perform(
            post("/tickets/entry")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"plate":"ABC-1234","parkingSpaceId":1}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST entry con rol USER devuelve 201 Created`() {
        val request = EntryRequest(plate = "ABC-1234", parkingSpaceId = 1L)
        val response = TicketResponse(id = 1L, plate = "ABC-1234", entryTime = LocalDateTime.now(), exitTime = null)
        `when`(ticketService.registerEntry(request)).thenReturn(response)

        mockMvc.perform(
            post("/tickets/entry")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"plate":"ABC-1234","parkingSpaceId":1}""")
        ).andExpect(status().isCreated)
    }

    // ───────────────────────────── POST /tickets/exit (USER) ─────────────────────────────

    @Test
    fun `POST exit sin token devuelve 401 Unauthorized`() {
        mockMvc.perform(
            post("/tickets/exit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticketId":1}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST exit con rol ADMIN devuelve 403 Forbidden`() {
        mockMvc.perform(
            post("/tickets/exit")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticketId":1}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST exit con rol USER devuelve 200 Ok`() {
        val request = ExitRequest(ticketId = 1L)
        val response = TicketResponse(id = 1L, plate = "ABC-1234", entryTime = LocalDateTime.now().minusHours(1), exitTime = LocalDateTime.now())
        `when`(ticketService.registerExit(request)).thenReturn(response)

        mockMvc.perform(
            post("/tickets/exit")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticketId":1}""")
        ).andExpect(status().isOk)
    }
}
