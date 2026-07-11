package com.pucetec.roles.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Seguridad del microservicio de estacionamiento con AUTORIZACIÓN POR ROLES de AWS Cognito.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // PÚBLICO: consultar espacios disponibles
                auth.requestMatchers(HttpMethod.GET, "/parking-spaces/available").permitAll()

                // SOLO ADMIN: crear espacios de estacionamiento
                auth.requestMatchers(HttpMethod.POST, "/parking-spaces").hasRole("ADMIN")

                // SOLO USER: registrar entrada y salida de vehículos (/tickets/entry y /tickets/exit)
                auth.requestMatchers(HttpMethod.POST, "/tickets/**").hasRole("USER")

                // Cualquier otra ruta exige token válido
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(cognitoGroupsConverter())
                }
            }
        return http.build()
    }

    /**
     * Lee el claim "cognito:groups" del access_token de Cognito y lo convierte en roles de Spring.
     * Un grupo de Cognito "ADMIN" se transforma en la autoridad "ROLE_ADMIN".
     */
    private fun cognitoGroupsConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val groups = jwt.getClaimAsStringList("cognito:groups") ?: emptyList()
            groups.map { SimpleGrantedAuthority("ROLE_$it") }
        }
        return converter
    }
}
