package com.planty.repository.diary;

import com.planty.entity.diary.Diary;
import com.planty.entity.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


// 재배일지 Repository
public interface DiaryRepository extends JpaRepository<Diary, Integer> {
    
    // 재배일지 상세 조회 (연관 엔티티 함께 로드)
    Optional<Diary> findById(Integer id);

    // 상세 전용 - 연관 한 번에 가져오기
    @EntityGraph(attributePaths = {"user", "crop", "images"})
    Optional<Diary> findDetailById(Integer id);

    // 작물별 재배일지 목록 조회 (최신순)
    @EntityGraph(attributePaths = {"user", "crop", "images"})
    List<Diary> findByCrop_IdOrderByCreatedAtDesc(Integer cropId);
}
