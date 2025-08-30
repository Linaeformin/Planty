package com.planty.repository.board;

import com.planty.dto.board.BoardAllResDto;
import com.planty.entity.board.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


// 판매 게시판 레포지토리
public interface BoardRepository extends JpaRepository<Board, Integer> {
    // 판매 게시글 상세 정보 가져오기
    @EntityGraph(attributePaths = {"user","crop","images"})
    Optional<Board> findById(Integer id);

    // 판매 게시글 전체 목록 가져오기
    List<Board> findAllByOrderByCreatedAtDesc();

    // 제목으로 판매 게시글 검색하기 TODO: 카테고리를 없애면서 확인 필수
    @Query("""
        select distinct b
        from Board b
        left join fetch b.images i
        where lower(b.title) like lower(:pattern)
        order by b.createdAt desc
    """)
    List<Board> searchByKeyword(@Param("pattern") String pattern);

    // 게시글 기준으로 crop 아이디 가져오기
    @Query("select b.crop.id from Board b where b.id = :boardId")
    Integer findCropIdByBoardId(@Param("boardId") Integer boardId);

    // 내가 쓴 판매 게시글 불러오기
    @Query("""
        select b
        from Board b
        left join fetch b.images
        where b.user.id = :userId
        order by b.sell asc, b.createdAt desc
    """)
    List<Board> findMyBoardsOrderByStatusAndCreated(@Param("userId") Integer userId);

}
