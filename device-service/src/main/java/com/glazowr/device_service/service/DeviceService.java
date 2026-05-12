package com.glazowr.device_service.service;

import com.glazowr.device_service.dto.DeviceDto;
import com.glazowr.device_service.entity.Device;
import com.glazowr.device_service.exception.DeviceNotFoundException;
import com.glazowr.device_service.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;

import java.util.List;

@Service
public class DeviceService {

    private DeviceRepository deviceRepository;

    private final CacheManager cacheManager;

    public DeviceService(DeviceRepository deviceRepository, CacheManager cacheManager) {
        this.deviceRepository = deviceRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = "deviceById", key = "#id")
    public DeviceDto getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new DeviceNotFoundException("Device not found with id " + id));
        return mapToDto(device);
    }

    public DeviceDto createDevice(DeviceDto input) {
        Device device = new Device();
        device.setName(input.getName());
        device.setType(input.getType());
        device.setLocation(input.getLocation());
        device.setUserId(input.getUserId());

        final Device savedDevice = deviceRepository.save(device);
        evictDeviceCaches(savedDevice.getId(), savedDevice.getUserId());
        return mapToDto(savedDevice);
    }

    public DeviceDto updateDevice(Long id, DeviceDto input) {
        Device existing = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new DeviceNotFoundException("Device not found with id " + id));

        Long oldUserId = existing.getUserId();
        existing.setName(input.getName());
        existing.setType(input.getType());
        existing.setLocation(input.getLocation());
        existing.setUserId(input.getUserId());

        final Device updatedDevice = deviceRepository.save(existing);
        evictDeviceCaches(updatedDevice.getId(), oldUserId);
        evictDeviceCaches(updatedDevice.getId(), updatedDevice.getUserId());
        return mapToDto(updatedDevice);
    }

    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new DeviceNotFoundException("Device not found with id " + id);
        }
        Device existing = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new DeviceNotFoundException("Device not found with id " + id));
        deviceRepository.deleteById(id);
        evictDeviceCaches(existing.getId(), existing.getUserId());
    }

    @Cacheable(value = "devicesByUserId", key = "#userId")
    public List<DeviceDto> getAllDevicesByUserId(Long userId) {
        List<Device> devices = deviceRepository.findAllByUserId(userId);
        return devices.stream()
                .map(this::mapToDto)
                .toList();
    }

    private void evictDeviceCaches(Long deviceId, Long userId) {
        evictCache("deviceById", deviceId);
        evictCache("devicesByUserId", userId);
    }

    private void evictCache(String cacheName, Long key) {
        if (key == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }


    private DeviceDto mapToDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setType(device.getType());
        dto.setLocation(device.getLocation());
        dto.setUserId(device.getUserId());
        return dto;
    }

}
