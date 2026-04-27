package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import com.sni.bokaticowork.core.retry.service.interfaces.RetryExecutor;
import com.sni.bokaticowork.features.ressource.dto.request.CreateAmenityRequest;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourceAmenitiesMapper;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenitiesRepository;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAmenitiesServiceImplTest {

    @Mock
    private ResourceAmenitiesRepository amenitiesRepository;

    @Mock
    private ResourceAmenitiesMapper amenitiesMapper;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private RetryExecutor retryExecutor;

    @Mock
    private RetryPolicy defaultRetryPolicy;

    @InjectMocks
    private ResourceAmenitiesServiceImpl service;

    @Test
    void shouldGenerateLongDynamicAmenityCodeFromAmenityName() {
        LocalDate today = LocalDate.now();
        CreateAmenityRequest request = CreateAmenityRequest.builder()
                .name("Wifi Access")
                .description("Fast wireless internet")
                .active(true)
                .build();
        ResourceAmenities amenity = ResourceAmenities.builder().build();

        when(amenitiesRepository.existsByNameIgnoreCase("Wifi Access")).thenReturn(false);
        when(amenitiesMapper.toEntity(any(CreateAmenityRequest.class))).thenReturn(amenity);
        when(sequenceGenerator.next("resource_amenity", today)).thenReturn("AMN-00027");
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<ResourceAmenities> action = invocation.getArgument(2);
            return action.get();
        }).when(retryExecutor).execute(eq("create resource amenity"), eq(defaultRetryPolicy), any());
        when(amenitiesRepository.save(any(ResourceAmenities.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createAmenity(request);

        ArgumentCaptor<ResourceAmenities> amenityCaptor = ArgumentCaptor.forClass(ResourceAmenities.class);
        verify(amenitiesRepository).save(amenityCaptor.capture());
        assertEquals("AMN-WIF-" + today.getYear() + String.format("%02d", today.getMonthValue()) + "-00000027",
                amenityCaptor.getValue().getCode());
    }
}
