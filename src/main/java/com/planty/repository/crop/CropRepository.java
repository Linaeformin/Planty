package com.planty.repository.crop;

import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CropRepository extends JpaRepository<Crop, Integer> {
    // 재배 완료된 작물 불러오기
    List<Crop> findByUser_IdAndHarvestTrueOrderByCreatedAtDesc(Integer userId);
}