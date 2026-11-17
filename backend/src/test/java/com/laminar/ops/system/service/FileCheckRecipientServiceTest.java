package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class FileCheckRecipientServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private FileCheckRecipientService service;

    @Test
    void rejectsInvalidEmail() {
        BizException ex = assertThrows(BizException.class,
                () -> service.save(Map.of("companyId", 1, "email", "not-an-email")));
        assertEquals(400, ex.getCode());
    }

    @Test
    void requiresCompanyOrHospital() {
        BizException ex = assertThrows(BizException.class,
                () -> service.save(Map.of("email", "ops@example.com")));
        assertEquals(400, ex.getCode());
    }
}
