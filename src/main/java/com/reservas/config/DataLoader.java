// src/main/java/com/reservas/config/DataLoader.java
package com.reservas.config;

import com.reservas.entity.*;
import com.reservas.enums.*;
import com.reservas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ComplexRepository complexRepository;
    private final CourtRepository courtRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ✅ Solo cargar datos si la BD está vacía
        if (userRepository.count() > 0) {
            log.info("⏭️ Datos ya cargados, saltando DataLoader");
            return;
        }

        log.info("📦 Cargando datos de prueba...");

        // Crear User Super Admin
        User adminUser = User.builder()
                .email("ma77acos@gmail.com")
                .password(passwordEncoder.encode("Ma77acos"))
                .displayName("Marcos Nardelli")
                .role(Role.ADMIN)
                .photoUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.2885-19/539473145_18521328877053783_4087855635494871171_n.jpg")
                .complex(null)
                .build();
        userRepository.save(adminUser);

        User playerUser = User.builder()
                .email("player@test.com")
                .password(passwordEncoder.encode("123456"))
                .displayName("Player Test")
                .role(Role.PLAYER)
                .photoUrl("https://cdn.site/player.jpg")
                .build();
        userRepository.save(playerUser);

        // Crear complejo
        Complex complex = Complex.builder()
                .name("Complejo Sunset Padel")
                .city("Necochea")
                .description("Complejo premium con 3 canchas sinteticas")
                .address("Calle 46 °3644 e/ 73 y 75")
                .phone("+5492262569214")
                .rating(new BigDecimal("4.8"))
                .imageUrl("https://srvprod.dondejuegoapp.com/api/imagenes/complejo-478/2.jpeg?1771535072581")
                .build();
        complex = complexRepository.save(complex);

        // Crear canchas
        Court court1 = Court.builder()
                .name("Cancha 1")
                .covered(true)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("10000"))
                .imageUrl("https://srvprod.dondejuegoapp.com/api/imagenes/complejo-478/1.jpeg?1771535072581")
                .complex(complex)
                .build();
        courtRepository.save(court1);

        Court court2 = Court.builder()
                .name("Cancha 2")
                .covered(false)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("9000"))
                .imageUrl("https://srvprod.dondejuegoapp.com/api/imagenes/complejo-478/1.jpeg?1771535072581")
                .complex(complex)
                .build();
        courtRepository.save(court2);

        Court court3 = Court.builder()
                .name("Cancha 3")
                .covered(true)
                .surface(Surface.CEMENTO)
                .price(new BigDecimal("8500"))
                .imageUrl("https://srvprod.dondejuegoapp.com/api/imagenes/complejo-478/1.jpeg?1771535072581")
                .complex(complex)
                .build();
        courtRepository.save(court3);

        // Crear usuarios
        User businessUserSunset = User.builder()
                .email("sunset@gmail.com")
                .password(passwordEncoder.encode("123456"))
                .displayName("Sunset Padel")
                .role(Role.BUSINESS)
                .photoUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.2885-19/53469461_354401258509700_7553859588236247040_n.jpg")
                .complex(complex)
                .build();
        userRepository.save(businessUserSunset);

        // Crear segundo complejo
        Complex complex2 = Complex.builder()
                .name("Chez Club")
                .city("Necochea")
                .description("Club Premium con varias canchas de tenis y padel")
                .address("Calle 46 entre 105 y 107")
                .phone("+5492262218187")
                .rating(new BigDecimal("4.9"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/623301595_18086732990277929_3881394325804997268_n.jpg")
                .build();
        complex2 = complexRepository.save(complex2);

        Court court4 = Court.builder()
                .name("Cancha Padel 1")
                .covered(true)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("10000"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/619498794_17993665730879044_4538789887969801930_n.jpg")
                .complex(complex2)
                .build();
        courtRepository.save(court4);

        Court court5 = Court.builder()
                .name("Cancha Padel 2")
                .covered(true)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("10000"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/619498794_17993665730879044_4538789887969801930_n.jpg")
                .complex(complex2)
                .build();
        courtRepository.save(court5);

        Court court6 = Court.builder()
                .name("Cancha Padel 3")
                .covered(true)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("10000"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/619498794_17993665730879044_4538789887969801930_n.jpg")
                .complex(complex2)
                .build();
        courtRepository.save(court6);

        // Crear usuarios
        User businessUserChez = User.builder()
                .email("chez@gmail.com")
                .password(passwordEncoder.encode("123456"))
                .displayName("Chez Club")
                .role(Role.BUSINESS)
                .photoUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.2885-19/53469461_354401258509700_7553859588236247040_n.jpg")
                .complex(complex2)
                .build();
        userRepository.save(businessUserChez);

        // Crear tercer complejo
        Complex complex3 = Complex.builder()
                .name("Nucleo Padel")
                .city("Necochea")
                .description("Club Premium con 2 increibles canchas de padel")
                .address("Av. 75 4346")
                .phone("+2262367248")
                .rating(new BigDecimal("4.9"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/619688318_17935205394149743_1464216910913387790_n.jpg")
                .build();
        complex3 = complexRepository.save(complex3);

        Court court7 = Court.builder()
                .name("Cancha Padel 1")
                .covered(true)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("10000"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/619498794_17993665730879044_4538789887969801930_n.jpg")
                .complex(complex3)
                .build();
        courtRepository.save(court7);

        Court court8 = Court.builder()
                .name("Cancha Padel 2")
                .covered(true)
                .surface(Surface.SINTETICO)
                .price(new BigDecimal("10000"))
                .imageUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.82787-15/619498794_17993665730879044_4538789887969801930_n.jpg")
                .complex(complex3)
                .build();
        courtRepository.save(court8);

        // Crear usuarios
        User businessUserNucleo = User.builder()
                .email("nucleo@gmail.com")
                .password(passwordEncoder.encode("123456"))
                .displayName("Nucleo Padel")
                .role(Role.BUSINESS)
                .photoUrl("https://instagram.fmdq6-1.fna.fbcdn.net/v/t51.2885-19/367669825_9649024141834147_4833970240716258483_n.jpg")
                .complex(complex3)
                .build();
        userRepository.save(businessUserNucleo);



        log.info("✅ Datos de prueba cargados correctamente");
        log.info("📧 Usuario Business: ma77acos@gmail.com / 123456");
        log.info("📧 Usuario Player: player@test.com / 123456");
        log.info("📧 Usuario Admin: admin@test.com / admin123");
    }
}