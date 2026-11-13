package com.sni.bokaticowork.security.admin.user.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.security.admin.user.dto.request.UpdateStaffProfileRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.StaffProfileResponse;
import com.sni.bokaticowork.security.admin.user.model.StaffProfile;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.StaffProfileRepository;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class StaffProfileServiceImpl implements StaffProfileService {

    /** Une signature est une petite image : au-dela, c'est une photo ou un document. */
    static final int MAX_SIGNATURE_CHARS = 400_000;
    private static final Pattern SIGNATURE = Pattern.compile("^data:image/(png|jpeg|jpg);base64,[A-Za-z0-9+/=\\s]+$");

    private final StaffProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Author currentAuthor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())
                || "anonymousUser".equals(authentication.getName())) {
            return Author.SYSTEM;
        }
        return authorOf(authentication.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Author authorOf(String login) {
        if (!StringUtils.hasText(login) || "SYSTEM".equals(login)) {
            return Author.SYSTEM;
        }
        return userRepository.findByEmailIgnoreCase(login.trim())
                .map(user -> new Author(user.getEmail(), displayName(user),
                        profileRepository.findByUserId(user.getId()).map(StaffProfile::getJobTitle).orElse(null)))
                .orElseGet(() -> new Author(login.trim(), login.trim(), null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> signatureOf(String login) {
        if (!StringUtils.hasText(login)) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(login.trim())
                .flatMap(user -> profileRepository.findByUserId(user.getId()))
                .map(StaffProfile::getSignatureImageBase64)
                .filter(StringUtils::hasText);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffProfileResponse get(String userCode) {
        Users user = user(userCode);
        return toResponse(user, profileRepository.findByUserId(user.getId()).orElse(null));
    }

    @Override
    public StaffProfileResponse update(String userCode, UpdateStaffProfileRequest request) {
        Users user = user(userCode);
        StaffProfile profile = profileOf(user);
        if (request.jobTitle() != null) {
            profile.setJobTitle(StringUtils.hasText(request.jobTitle()) ? request.jobTitle().trim() : null);
        }
        if (request.managerUserCode() != null) {
            profile.setManagerUserId(StringUtils.hasText(request.managerUserCode())
                    ? checkedManager(user, user(request.managerUserCode().trim())) : null);
        }
        if (Boolean.TRUE.equals(request.clearSignature())) {
            profile.setSignatureImageBase64(null);
            profile.setSignatureUpdatedAt(Instant.now());
        } else if (StringUtils.hasText(request.signatureImageBase64())) {
            profile.setSignatureImageBase64(checkedSignature(request.signatureImageBase64()));
            profile.setSignatureUpdatedAt(Instant.now());
        }
        return toResponse(user, profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffProfileResponse me() {
        Users user = currentUser();
        return toResponse(user, profileRepository.findByUserId(user.getId()).orElse(null));
    }

    @Override
    public StaffProfileResponse updateMySignature(String signatureImageBase64) {
        Users user = currentUser();
        StaffProfile profile = profileOf(user);
        profile.setSignatureImageBase64(checkedSignature(signatureImageBase64));
        profile.setSignatureUpdatedAt(Instant.now());
        return toResponse(user, profileRepository.save(profile));
    }

    @Override
    public StaffProfileResponse clearMySignature() {
        Users user = currentUser();
        StaffProfile profile = profileOf(user);
        profile.setSignatureImageBase64(null);
        profile.setSignatureUpdatedAt(Instant.now());
        return toResponse(user, profileRepository.save(profile));
    }

    /** Le superieur ne peut etre ni l'utilisateur lui-meme ni l'un de ses subordonnes (boucle). */
    private Long checkedManager(Users user, Users manager) {
        if (manager.getId() == user.getId()) {
            throw new BadRequestException("Un collaborateur ne peut pas être son propre supérieur");
        }
        Set<Long> seen = new HashSet<>();
        Long cursor = manager.getId();
        while (cursor != null && seen.add(cursor)) {
            if (cursor == user.getId()) {
                throw new BadRequestException("Ce choix crée une boucle dans la hiérarchie : "
                        + displayName(manager) + " dépend déjà de " + displayName(user));
            }
            cursor = profileRepository.findByUserId(cursor).map(StaffProfile::getManagerUserId).orElse(null);
        }
        return manager.getId();
    }

    static String checkedSignature(String value) {
        String signature = value == null ? "" : value.trim();
        if (signature.length() > MAX_SIGNATURE_CHARS) {
            throw new BadRequestException("La signature est trop lourde : 300 Ko au plus");
        }
        if (!SIGNATURE.matcher(signature).matches()) {
            throw new BadRequestException("La signature doit être une image PNG ou JPEG en data URI (data:image/png;base64,...)");
        }
        return signature;
    }

    private StaffProfile profileOf(Users user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> StaffProfile.builder().userId(user.getId()).build());
    }

    private Users user(String userCode) {
        return userRepository.findByUserId(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + userCode));
    }

    private Users currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication == null ? null : authentication.getName();
        return Optional.ofNullable(login).flatMap(userRepository::findByEmailIgnoreCase)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
    }

    private StaffProfileResponse toResponse(Users user, StaffProfile profile) {
        Users manager = profile == null || profile.getManagerUserId() == null ? null
                : userRepository.findById(profile.getManagerUserId()).orElse(null);
        String signature = profile == null ? null : profile.getSignatureImageBase64();
        return new StaffProfileResponse(user.getUserId(), user.getEmail(), displayName(user),
                profile == null ? null : profile.getJobTitle(),
                StringUtils.hasText(signature), signature,
                profile == null ? null : profile.getSignatureUpdatedAt(),
                manager == null ? null : manager.getUserId(),
                manager == null ? null : displayName(manager),
                manager == null ? null : manager.getEmail());
    }

    static String displayName(Users user) {
        String name = ((user.getFirstname() == null ? "" : user.getFirstname().trim()) + " "
                + (user.getLastname() == null ? "" : user.getLastname().trim())).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }
}
