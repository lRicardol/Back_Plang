package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.BadRequestException;
import com.tourism.platform.exception.ConflictException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.ConnectionStatus;
import com.tourism.platform.model.SharedActivity;
import com.tourism.platform.model.SharedActivityDecisionAction;
import com.tourism.platform.model.SharedActivityStatus;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanActivity;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.SharedActivityRepository;
import com.tourism.platform.repository.TravelPlanActivityRepository;
import com.tourism.platform.repository.ConnectionRepository;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedActivityServiceImplTest {

    @Mock
    private TravelPlanActivityRepository activityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SharedActivityRepository sharedActivityRepository;
    @Mock
    private ConnectionRepository connectionRepository;

    private SharedActivityServiceImpl service;

    private User sender;
    private User receiver;
    private TravelPlanActivity activity;

    @BeforeEach
    void setUp() {
        service = new SharedActivityServiceImpl(
                activityRepository,
                userRepository,
                sharedActivityRepository,
                connectionRepository,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        );

        sender = new User();
        sender.setId(10L);
        sender.setUsername("sender");

        receiver = new User();
        receiver.setId(20L);
        receiver.setUsername("receiver");

        TravelPlan trip = new TravelPlan();
        trip.setId(99L);
        trip.setUser(sender);

        activity = new TravelPlanActivity();
        activity.setId(7L);
        activity.setTravelPlan(trip);
        activity.setStartTime(LocalDateTime.now());
        activity.setEndTime(LocalDateTime.now().plusHours(2));
    }

    @Test
    void shareActivity_shouldPreventSharingToSameUser() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sender));

        assertThrows(BadRequestException.class, () -> service.shareActivity(7L, 10L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenSenderNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.shareActivity(7L, 20L, "ghost"));
    }

    @Test
    void shareActivity_throwsWhenActivityNotFound() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenReceiverNotFound() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenTripContextInvalid() {
        TravelPlanActivity orphan = new TravelPlanActivity();
        orphan.setId(7L);
        orphan.setTravelPlan(null);

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(orphan));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenTripIdMissing() {
        TravelPlan bare = new TravelPlan();
        bare.setUser(sender);
        TravelPlanActivity a = new TravelPlanActivity();
        a.setId(7L);
        a.setTravelPlan(bare);

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(a));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenOwnerUnknownOnTrip() {
        TravelPlan trip = new TravelPlan();
        trip.setId(99L);
        trip.setUser(null);
        TravelPlanActivity a = new TravelPlanActivity();
        a.setId(7L);
        a.setTravelPlan(trip);

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(a));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenReceiverNotOnTrip() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_succeedsWhenEligible() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(true);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(7L, 20L))
                .thenReturn(Optional.empty());
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(inv -> {
            SharedActivity sa = inv.getArgument(0);
            ReflectionTestUtils.setField(sa, "id", 55L);
            return sa;
        });

        SharedActivityResponse response = service.shareActivity(7L, 20L, "sender");

        assertEquals(55L, response.getId());
        assertEquals(SharedActivityStatus.PENDING, response.getStatus());
        assertFalse(response.isSharedPlan());
    }

    @Test
    void shareActivity_shouldRejectWhenSenderIsNotOwner() {
        User nonOwner = new User();
        nonOwner.setId(11L);
        nonOwner.setUsername("nonOwner");
        when(userRepository.findByUsername("nonOwner")).thenReturn(Optional.of(nonOwner));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "nonOwner"));
    }

    @Test
    void shareActivity_shouldRejectDuplicatePendingShare() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(true);
        SharedActivity existing = new SharedActivity();
        ReflectionTestUtils.setField(existing, "id", 77L);
        existing.setActivity(activity);
        existing.setSender(sender);
        existing.setReceiver(receiver);
        existing.setStatus(SharedActivityStatus.PENDING);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(7L, 20L))
                .thenReturn(Optional.of(existing));

        SharedActivityResponse response = service.shareActivity(7L, 20L, "sender");
        assertEquals(77L, response.getId());
        assertEquals(SharedActivityStatus.PENDING, response.getStatus());
    }

    @Test
    void shareActivity_shouldRejectWhenConnectionIsNotAccepted() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_shouldRejectWhenAlreadyAccepted() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(true);
        SharedActivity existing = new SharedActivity();
        ReflectionTestUtils.setField(existing, "id", 78L);
        existing.setActivity(activity);
        existing.setSender(sender);
        existing.setReceiver(receiver);
        existing.setStatus(SharedActivityStatus.ACCEPTED);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(7L, 20L))
                .thenReturn(Optional.of(existing));

        SharedActivityResponse response = service.shareActivity(7L, 20L, "sender");
        assertEquals(78L, response.getId());
        assertEquals(SharedActivityStatus.ACCEPTED, response.getStatus());
    }

    @Test
    void resolveSharedActivity_shouldAcceptPendingShare() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resolveSharedActivity(1L, request, "receiver");
        assertEquals(SharedActivityStatus.ACCEPTED, response.getStatus());
        assertTrue(response.isSharedPlan());
    }

    @Test
    void resolveSharedActivity_shouldRejectWhenAlreadyResolved() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.ACCEPTED);

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.REJECT);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));

        assertThrows(ConflictException.class, () -> service.resolveSharedActivity(1L, request, "receiver"));
    }

    @Test
    void resolveSharedActivity_shouldRejectPendingShare() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.REJECT);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SharedActivityResponse response = service.resolveSharedActivity(1L, request, "receiver");

        assertEquals(SharedActivityStatus.REJECTED, response.getStatus());
        assertFalse(response.isSharedPlan());
    }

    @Test
    void resolveSharedActivity_throwsWhenReceiverNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);
        assertThrows(ResourceNotFoundException.class, () -> service.resolveSharedActivity(1L, request, "ghost"));
    }

    @Test
    void resolveSharedActivity_throwsWhenShareNotFound() {
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.empty());
        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);
        assertThrows(ResourceNotFoundException.class, () -> service.resolveSharedActivity(1L, request, "receiver"));
    }

    @Test
    void resolveSharedActivity_throwsWhenCallerIsNotReceiver() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);

        User other = new User();
        other.setId(99L);
        other.setUsername("other");

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));

        assertThrows(AccessDeniedException.class, () -> service.resolveSharedActivity(1L, request, "other"));
    }

    @Test
    void getSharedActivities_returnsSentAndReceivedWithActivityDetails() {
        activity.setName("Museo");
        activity.setDescription("Tour guiado");
        activity.setLocation("Roma");

        SharedActivity received = new SharedActivity();
        ReflectionTestUtils.setField(received, "id", 1L);
        received.setActivity(activity);
        received.setSender(sender);
        received.setReceiver(receiver);
        received.setStatus(SharedActivityStatus.PENDING);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findByReceiverIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(received));
        when(sharedActivityRepository.findBySenderIdOrderByCreatedAtDesc(20L)).thenReturn(List.of());

        List<SharedActivityResponse> responses = service.getSharedActivities("receiver");

        assertEquals(1, responses.size());
        assertEquals("Museo", responses.get(0).getActivityName());
        assertEquals("Roma", responses.get(0).getActivityLocation());
    }

    @Test
    void shareAllActivities_returnsOneSharePerPlanActivity() {
        TravelPlanActivity second = new TravelPlanActivity();
        second.setId(8L);
        second.setTravelPlan(activity.getTravelPlan());
        second.setName("Cena");
        second.setStartTime(LocalDateTime.now());
        second.setEndTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findByTravelPlanIdOrderByStartTimeAsc(99L))
                .thenReturn(List.of(activity, second));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(activityRepository.findById(8L)).thenReturn(Optional.of(second));
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED))
                .thenReturn(true);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> {
            SharedActivity sharedActivity = invocation.getArgument(0);
            if (sharedActivity.getActivity().getId().equals(7L)) {
                ReflectionTestUtils.setField(sharedActivity, "id", 70L);
            } else {
                ReflectionTestUtils.setField(sharedActivity, "id", 80L);
            }
            return sharedActivity;
        });

        List<SharedActivityResponse> responses = service.shareAllActivities(99L, 20L, "sender");

        assertEquals(2, responses.size());
    }
}
