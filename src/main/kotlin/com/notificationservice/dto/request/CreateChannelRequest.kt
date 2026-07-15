package com.notificationservice.dto.request

import com.notificationservice.domain.enums.ChannelType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Request body for creating a new Channel under a Tenant.
 */
data class CreateChannelRequest(

    @field:NotNull(message = "Channel type must not be null")
    val channelType: ChannelType,

    @field:NotBlank(message = "Name must not be blank")
    @field:Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    val name: String,

    /**
     * Channel-specific configuration key-value pairs.
     * Example for EMAIL: { "smtpHost": "smtp.example.com", "fromAddress": "no-reply@example.com" }
     */
    val config: Map<String, Any> = emptyMap()
)
