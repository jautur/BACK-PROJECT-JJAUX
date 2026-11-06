package com.avisa;

import com.avisa.config.AdminSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Comprova que el context de Spring es munta. Els tests usen H2 sense esquema (les migracions són de PostgreSQL),
 * així que AdminSeeder, que s'executa en tots els perfils i consulta usuaris en arrencar, es substitueix per un mock.
 * El seu comportament real el prova AdminSeederIT sobre PostgreSQL.
 */
@SpringBootTest(classes = AvisaApplication.class)
class AvisaApplicationTests {

    @MockBean
    AdminSeeder adminSeeder;

    @Test
    void contextLoads() {
    }

}
