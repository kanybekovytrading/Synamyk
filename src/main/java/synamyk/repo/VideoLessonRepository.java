package synamyk.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import synamyk.entities.VideoLesson;

import java.util.List;

@Repository
public interface VideoLessonRepository extends JpaRepository<VideoLesson, Long> {

    List<VideoLesson> findByActiveTrueOrderByOrderIndexAsc();

    List<VideoLesson> findByTestIdAndActiveTrueOrderByOrderIndexAsc(Long testId);
}