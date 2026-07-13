package com.whatiread.instance.repository;

import com.whatiread.instance.domain.InstanceSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstanceSettingRepository extends JpaRepository<InstanceSetting, String> {
}
