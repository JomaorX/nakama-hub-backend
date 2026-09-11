package com.nakamahub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * La autenticación va por JWT contra la tabla de usuarios, así que no se usa el
 * UserDetailsService de Spring. Sin esta exclusión, Boot crea un usuario en memoria
 * con una contraseña aleatoria y la escribe en el log en cada arranque.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class NakamaHubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NakamaHubBackendApplication.class, args);
	}

}
