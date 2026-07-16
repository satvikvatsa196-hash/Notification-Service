package com.notificationservice.unit

import com.notificationservice.domain.model.Tenant
import com.notificationservice.dto.request.CreateTenantRequest
import com.notificationservice.exception.DuplicateResourceException
import com.notificationservice.exception.ResourceNotFoundException
import com.notificationservice.repository.TenantRepository
import com.notificationservice.service.TenantService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(SpringExtension::class)
class TenantServiceTest {

    private val tenantRepository: TenantRepository = mockk()
    private lateinit var tenantService: TenantService

    @BeforeEach
    fun setUp() {
        tenantService = TenantService(tenantRepository)
    }

    @Test
    fun `createTenant should create and return tenant when no duplicates exist`() {
        val request = CreateTenantRequest(
            name = "Acme Corp",
            slug = "acme-corp",
            contactEmail = "admin@acme.com"
        )

        every { tenantRepository.existsByName(request.name) } returns false
        every { tenantRepository.existsBySlug(request.slug) } returns false
        every { tenantRepository.save(any()) } answers {
            firstArg<Tenant>().apply { id = UUID.randomUUID() }
        }

        val result = tenantService.createTenant(request)

        assertThat(result.name).isEqualTo("Acme Corp")
        assertThat(result.slug).isEqualTo("acme-corp")
        assertThat(result.contactEmail).isEqualTo("admin@acme.com")
        assertThat(result.active).isTrue()
        verify(exactly = 1) { tenantRepository.save(any()) }
    }

    @Test
    fun `createTenant should throw DuplicateResourceException when name is taken`() {
        val request = CreateTenantRequest(
            name = "Existing Corp",
            slug = "new-slug",
            contactEmail = "admin@existing.com"
        )

        every { tenantRepository.existsByName(request.name) } returns true

        assertThatThrownBy { tenantService.createTenant(request) }
            .isInstanceOf(DuplicateResourceException::class.java)
            .hasMessageContaining("Existing Corp")

        verify(exactly = 0) { tenantRepository.save(any()) }
    }

    @Test
    fun `createTenant should throw DuplicateResourceException when slug is taken`() {
        val request = CreateTenantRequest(
            name = "Unique Name",
            slug = "taken-slug",
            contactEmail = "admin@unique.com"
        )

        every { tenantRepository.existsByName(request.name) } returns false
        every { tenantRepository.existsBySlug(request.slug) } returns true

        assertThatThrownBy { tenantService.createTenant(request) }
            .isInstanceOf(DuplicateResourceException::class.java)
            .hasMessageContaining("taken-slug")
    }

    @Test
    fun `getTenantById should throw ResourceNotFoundException when tenant does not exist`() {
        val id = UUID.randomUUID()
        every { tenantRepository.findById(id) } returns Optional.empty()

        assertThatThrownBy { tenantService.getTenantById(id) }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining(id.toString())
    }

    @Test
    fun `listTenants should return empty list when no active tenants exist`() {
        val pageable = PageRequest.of(0, 20)
        every { tenantRepository.findAllActive(pageable) } returns PageImpl(emptyList())

        val (tenants, meta) = tenantService.listTenants(pageable)

        assertThat(tenants).isEmpty()
        assertThat(meta.totalElements).isZero()
    }
}
