package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.MembershipGroupConstants;
import com.portal.universe.authservice.auth.domain.RoleDefaultMembership;
import com.portal.universe.authservice.auth.dto.rbac.RoleDefaultMappingRequest;
import com.portal.universe.authservice.auth.dto.rbac.RoleDefaultMappingResponse;
import com.portal.universe.authservice.auth.repository.MembershipTierRepository;
import com.portal.universe.authservice.auth.repository.RoleDefaultMembershipRepository;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleDefaultMembershipService {

    private final RoleDefaultMembershipRepository roleDefaultMembershipRepository;
    private final RoleEntityRepository roleEntityRepository;
    private final MembershipTierRepository membershipTierRepository;

    public List<RoleDefaultMappingResponse> getAllMappings() {
        return roleDefaultMembershipRepository.findAll().stream()
                .map(RoleDefaultMappingResponse::from)
                .toList();
    }

    public List<RoleDefaultMappingResponse> getMappingsByRoleKey(String roleKey) {
        return roleDefaultMembershipRepository.findByRoleKey(roleKey).stream()
                .map(RoleDefaultMappingResponse::from)
                .toList();
    }

    @Transactional
    public RoleDefaultMappingResponse addMapping(RoleDefaultMappingRequest request, String adminId) {
        // roleKey 존재 검증
        if (!roleEntityRepository.existsByRoleKey(request.roleKey())) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND);
        }

        // membershipGroup 포맷 검증
        MembershipGroupConstants.validate(request.membershipGroup());

        // tier 존재 검증
        if (!membershipTierRepository.existsByMembershipGroupAndTierKey(
                request.membershipGroup(), request.defaultTierKey())) {
            throw new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND);
        }

        // 중복 검증
        if (roleDefaultMembershipRepository.existsByRoleKeyAndMembershipGroup(
                request.roleKey(), request.membershipGroup())) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_DEFAULT_MAPPING_ALREADY_EXISTS);
        }

        RoleDefaultMembership mapping = RoleDefaultMembership.builder()
                .roleKey(request.roleKey())
                .membershipGroup(request.membershipGroup())
                .defaultTierKey(request.defaultTierKey())
                .build();

        RoleDefaultMembership saved = roleDefaultMembershipRepository.save(mapping);

        log.info("Role default mapping added: role={}, group={}, tier={}, by={}",
                request.roleKey(), request.membershipGroup(), request.defaultTierKey(), adminId);
        return RoleDefaultMappingResponse.from(saved);
    }

    @Transactional
    public void removeMapping(String roleKey, String membershipGroup, String adminId) {
        if (!roleDefaultMembershipRepository.existsByRoleKeyAndMembershipGroup(roleKey, membershipGroup)) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_DEFAULT_MAPPING_NOT_FOUND);
        }

        roleDefaultMembershipRepository.deleteByRoleKeyAndMembershipGroup(roleKey, membershipGroup);

        log.info("Role default mapping removed: role={}, group={}, by={}", roleKey, membershipGroup, adminId);
    }
}
