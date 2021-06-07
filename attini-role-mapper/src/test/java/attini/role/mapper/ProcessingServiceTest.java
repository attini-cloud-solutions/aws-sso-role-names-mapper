package attini.role.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {
    @Mock
    Test1 test;

    ProcessingService processingService;

    @BeforeEach
    public void setup() {
        processingService = new ProcessingService(test);
    }

    @Test
    public void testString() {
        when(test.getString()).thenReturn("hej");
        assertEquals("hej", processingService.testing());
    }
}