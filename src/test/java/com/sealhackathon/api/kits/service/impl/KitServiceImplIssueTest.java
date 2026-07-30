package com.sealhackathon.api.kits.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.kits.dto.request.IssueKitRequest;
import com.sealhackathon.api.kits.dto.request.RevokeKitRequest;
import com.sealhackathon.api.kits.entity.KitAllocation;
import com.sealhackathon.api.kits.entity.KitItem;
import com.sealhackathon.api.kits.entity.KitStock;
import com.sealhackathon.api.kits.mapper.KitMapper;
import com.sealhackathon.api.kits.repository.KitAllocationRepository;
import com.sealhackathon.api.kits.repository.KitItemRepository;
import com.sealhackathon.api.kits.repository.KitStockRepository;
import com.sealhackathon.api.kits.value_object.KitAllocationStatus;
import com.sealhackathon.api.kits.value_object.KitItemType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitServiceImplIssueTest {

    @Mock KitItemRepository kitItemRepository;
    @Mock KitStockRepository kitStockRepository;
    @Mock KitAllocationRepository kitAllocationRepository;
    @Mock HackathonRepository hackathonRepository;
    @Mock HackathonRegistrationRepository hackathonRegistrationRepository;
    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock UserRepository userRepository;
    @Mock EventRepository eventRepository;
    @Mock KitMapper kitMapper;
    @Mock AuditService auditService;
    @Mock CurrentUserAccessor currentUserAccessor;
    @Spy HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks KitServiceImpl kitService;

    Hackathon hackathon;
    User student;
    User coord;
    Team team;
    KitItem shirt;
    KitStock stock;

    @BeforeEach
    void setUp() {
        hackathon = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).build();
        student = User.builder().id(10).fullName("A").studentCode("SE123").build();
        coord = User.builder().id(99).fullName("Coord").build();
        team = Team.builder().id(5).hackathon(hackathon).teamName("Alpha").build();
        shirt = KitItem.builder().id(7).hackathon(hackathon).name("Áo").type(KitItemType.SHIRT).hasSize(true).build();
        stock = KitStock.builder().id(3).kitItem(shirt).size("M").sizeKey("M")
                .quantityTotal(10).quantityIssued(0).version(0L).build();
    }

    private void stubEligibleRecipient() {
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(hackathon));
        when(userRepository.findById(10)).thenReturn(Optional.of(student));
        when(kitItemRepository.findById(7)).thenReturn(Optional.of(shirt));
        when(teamRepository.findByHackathon_IdAndStatus(1, TeamStatus.ACTIVE)).thenReturn(List.of(team));
        TeamMember member = TeamMember.builder().team(team).user(student).status(TeamMemberStatus.ACCEPTED).build();
        when(teamMemberRepository.findByTeam_IdIn(List.of(5))).thenReturn(List.of(member));
        lenient().when(hackathonRegistrationRepository.findByHackathon_IdAndUser_Id(1, 10))
                .thenReturn(Optional.of(HackathonRegistration.builder()
                        .hackathon(hackathon).user(student).preferredShirtSize("M").build()));
        lenient().when(currentUserAccessor.currentUserId()).thenReturn(99);
        lenient().when(userRepository.findById(99)).thenReturn(Optional.of(coord));
        lenient().when(eventRepository.findByHackathonIdAndType(anyInt(), any())).thenReturn(List.of());
        lenient().when(kitMapper.toAllocationResponse(any())).thenAnswer(inv -> null);
    }

    @Test
    void issue_whenStockZero_throwsOutOfStock() {
        stubEligibleRecipient();
        stock.setQuantityIssued(10);
        when(kitAllocationRepository.findByHackathon_IdAndUser_IdAndKitItem_Id(1, 10, 7))
                .thenReturn(Optional.empty());
        when(kitStockRepository.findByKitItem_IdAndSizeKey(7, "M")).thenReturn(Optional.of(stock));

        IssueKitRequest req = IssueKitRequest.builder().userId(10).kitItemId(7).build();

        assertThatThrownBy(() -> kitService.issue(1, req))
                .isInstanceOf(BusinessRuleException.class)
                .matches(ex -> ErrorCode.KIT_OUT_OF_STOCK.equals(((BusinessRuleException) ex).getCode()));
    }

    @Test
    void issue_whenAlreadyIssued_throwsAlreadyIssued() {
        stubEligibleRecipient();
        KitAllocation existing = KitAllocation.builder()
                .id(44).hackathon(hackathon).user(student).kitItem(shirt)
                .status(KitAllocationStatus.ISSUED).size("M").build();
        when(kitAllocationRepository.findByHackathon_IdAndUser_IdAndKitItem_Id(1, 10, 7))
                .thenReturn(Optional.of(existing));

        IssueKitRequest req = IssueKitRequest.builder().userId(10).kitItemId(7).build();

        assertThatThrownBy(() -> kitService.issue(1, req))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.KIT_ALREADY_ISSUED.equals(((ConflictException) ex).getCode()));
    }

    @Test
    void revoke_restoresStock() {
        KitAllocation issued = KitAllocation.builder()
                .id(44).hackathon(hackathon).user(student).kitItem(shirt)
                .status(KitAllocationStatus.ISSUED).size("M").build();
        stock.setQuantityIssued(4);

        when(kitAllocationRepository.findById(44)).thenReturn(Optional.of(issued));
        when(kitStockRepository.findByKitItem_IdAndSizeKey(7, "M")).thenReturn(Optional.of(stock));
        when(kitAllocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(kitMapper.toAllocationResponse(any())).thenAnswer(inv -> null);

        kitService.revoke(44, RevokeKitRequest.builder().reason("Sai size").build());

        ArgumentCaptor<KitStock> stockCaptor = ArgumentCaptor.forClass(KitStock.class);
        verify(kitStockRepository).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantityIssued()).isEqualTo(3);

        ArgumentCaptor<KitAllocation> allocCaptor = ArgumentCaptor.forClass(KitAllocation.class);
        verify(kitAllocationRepository).save(allocCaptor.capture());
        assertThat(allocCaptor.getValue().getStatus()).isEqualTo(KitAllocationStatus.REVOKED);
        assertThat(allocCaptor.getValue().getNote()).isEqualTo("Sai size");
    }
}
