package com.supriyoroy.sportsmedia.service;

import com.supriyoroy.sportsmedia.model.SiteSetting;
import com.supriyoroy.sportsmedia.repo.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SiteSettingRepository repo;

    public Map<String, String> all() {
        Map<String, String> map = new LinkedHashMap<>();
        repo.findAll().forEach(s -> map.put(s.getKey(), s.getValue()));
        return map;
    }

    public String get(String key, String fallback) {
        return repo.findById(key).map(SiteSetting::getValue).orElse(fallback);
    }

    public void put(String key, String value) {
        repo.save(SiteSetting.builder().key(key).value(value).build());
    }

    public void putAll(Map<String, String> values) {
        values.forEach(this::put);
    }

    /** Only writes if the key has never been set, so seeding never clobbers your edits. */
    public void seed(String key, String value) {
        if (repo.findById(key).isEmpty()) put(key, value);
    }
}
