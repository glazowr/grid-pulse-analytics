package com.glazowr.user_service.service;

import com.glazowr.user_service.dto.UserDto;
import com.glazowr.user_service.entity.User;
import com.glazowr.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.glazowr.user_service.exception.UserNotFoundException;
import com.glazowr.user_service.exception.DuplicateEmailException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public UserService(UserRepository userRepository, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    public UserDto createUser(UserDto input) {

        if (userRepository.existsByEmail(input.getEmail())) {
            throw new DuplicateEmailException(input.getEmail());
        }

        final User createdUser = User.builder()
                .name(input.getName())
                .surname(input.getSurname())
                .email(input.getEmail())
                .address(input.getAddress())
                .alerting(input.isAlerting())
                .energyAlertingThreshold(input.getEnergyAlertingThreshold())
                .build();

        final User saved = userRepository.save(createdUser);
        evictUserCache(saved.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userById", key = "#id")
    public UserDto getUserById(Long id) {
        log.info("Fetching user from DB: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return toDto(user);
    }

    public void updateUser(Long id, UserDto dto) {

        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEmailException(dto.getEmail());
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        user.setAlerting(dto.isAlerting());
        user.setEnergyAlertingThreshold(dto.getEnergyAlertingThreshold());

        userRepository.save(user);
        evictUserCache(id);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
        evictUserCache(user.getId());
    }

    private void evictUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        Cache cache = cacheManager.getCache("userById");
        if (cache != null) {
            cache.evict(userId);
            log.info("Evicted cache for userId={}", userId);
        }
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .address(user.getAddress())
                .alerting(user.isAlerting())
                .energyAlertingThreshold(user.getEnergyAlertingThreshold())
                .build();
    }
}
