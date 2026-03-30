package synamyk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.dto.admin.*;
import synamyk.entities.*;
import synamyk.exception.AppException;
import synamyk.repo.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTestService {

    private final TestRepository testRepository;
    private final SubTestRepository subTestRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository optionRepository;

    // ===== TESTS =====

    public List<AdminTestResponse> getAllTests() {
        return testRepository.findAll().stream()
                .map(this::toAdminTestResponse)
                .toList();
    }

    public AdminTestResponse getTest(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException("Тест не найден.", "Тест табылган жок."));
        return toAdminTestResponse(test);
    }

    @Transactional
    public AdminTestResponse createTest(CreateTestRequest request) {
        Test test = Test.builder()
                .title(request.getTitle())
                .titleKy(request.getTitleKy())
                .description(request.getDescription())
                .descriptionKy(request.getDescriptionKy())
                .iconUrl(request.getIconUrl())
                .price(request.getPrice())
                .active(true)
                .build();
        return toAdminTestResponse(testRepository.save(test));
    }

    @Transactional
    public AdminTestResponse updateTest(Long testId, CreateTestRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException("Тест не найден.", "Тест табылган жок."));
        test.setTitle(request.getTitle());
        test.setTitleKy(request.getTitleKy());
        test.setDescription(request.getDescription());
        test.setDescriptionKy(request.getDescriptionKy());
        test.setIconUrl(request.getIconUrl());
        test.setPrice(request.getPrice());
        return toAdminTestResponse(testRepository.save(test));
    }

    @Transactional
    public void deleteTest(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException("Тест не найден.", "Тест табылган жок."));
        test.setActive(false);
        testRepository.save(test);
    }

    /**
     * Admin sets which sub-tests are paid and the price for the whole test.
     * Sub-tests NOT in paidSubTestIds will be marked as free.
     */
    @Transactional
    public AdminTestResponse updateTestPricing(Long testId, UpdateTestPricingRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException("Тест не найден.", "Тест табылган жок."));

        test.setPrice(request.getPrice());
        testRepository.save(test);

        List<SubTest> subTests = subTestRepository.findByTestIdOrderByLevelOrderAsc(testId);
        for (SubTest st : subTests) {
            st.setIsPaid(request.getPaidSubTestIds().contains(st.getId()));
            subTestRepository.save(st);
        }

        log.info("Updated pricing for testId={}: price={}, paidSubTests={}",
                testId, request.getPrice(), request.getPaidSubTestIds());

        return toAdminTestResponse(test);
    }

    // ===== SUB-TESTS =====

    @Transactional
    public AdminTestResponse.AdminSubTestResponse createSubTest(Long testId, CreateSubTestRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException("Тест не найден.", "Тест табылган жок."));

        SubTest subTest = SubTest.builder()
                .test(test)
                .title(request.getTitle())
                .titleKy(request.getTitleKy())
                .levelName(request.getLevelName())
                .levelNameKy(request.getLevelNameKy())
                .levelOrder(request.getLevelOrder())
                .isPaid(request.getIsPaid() != null && request.getIsPaid())
                .durationMinutes(request.getDurationMinutes())
                .active(true)
                .build();

        subTest = subTestRepository.save(subTest);
        return toAdminSubTestResponse(subTest);
    }

    @Transactional
    public AdminTestResponse.AdminSubTestResponse updateSubTest(Long subTestId, CreateSubTestRequest request) {
        SubTest subTest = subTestRepository.findById(subTestId)
                .orElseThrow(() -> new AppException("Подтест не найден.", "Подтест табылган жок."));

        subTest.setTitle(request.getTitle());
        subTest.setTitleKy(request.getTitleKy());
        subTest.setLevelName(request.getLevelName());
        subTest.setLevelNameKy(request.getLevelNameKy());
        subTest.setLevelOrder(request.getLevelOrder());
        subTest.setIsPaid(request.getIsPaid() != null && request.getIsPaid());
        subTest.setDurationMinutes(request.getDurationMinutes());

        return toAdminSubTestResponse(subTestRepository.save(subTest));
    }

    @Transactional
    public void deleteSubTest(Long subTestId) {
        SubTest subTest = subTestRepository.findById(subTestId)
                .orElseThrow(() -> new AppException("Подтест не найден.", "Подтест табылган жок."));
        subTest.setActive(false);
        subTestRepository.save(subTest);
    }

    // ===== QUESTIONS =====

    public List<AdminQuestionResponse> getQuestions(Long subTestId) {
        return questionRepository.findBySubTestIdOrderByOrderIndexAsc(subTestId).stream()
                .map(this::toAdminQuestionResponse)
                .toList();
    }

    @Transactional
    public AdminQuestionResponse createQuestion(Long subTestId, CreateQuestionRequest request) {
        SubTest subTest = subTestRepository.findById(subTestId)
                .orElseThrow(() -> new AppException("Подтест не найден.", "Подтест табылган жок."));

        boolean hasCorrect = request.getOptions().stream()
                .anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect()));
        if (!hasCorrect) {
            throw new AppException("Хотя бы один вариант должен быть отмечен как правильный.", "Жок дегенде бир туура жооп белгиленүү керек.");
        }

        Question question = Question.builder()
                .subTest(subTest)
                .text(request.getText())
                .textKy(request.getTextKy())
                .sectionName(request.getSectionName())
                .sectionNameKy(request.getSectionNameKy())
                .imageUrl(request.getImageUrl())
                .explanation(request.getExplanation())
                .explanationKy(request.getExplanationKy())
                .orderIndex(request.getOrderIndex())
                .pointValue(request.getPointValue())
                .active(true)
                .build();

        question = questionRepository.save(question);

        int optIndex = 0;
        for (CreateQuestionRequest.AnswerOptionRequest optReq : request.getOptions()) {
            AnswerOption option = AnswerOption.builder()
                    .question(question)
                    .label(optReq.getLabel())
                    .text(optReq.getText())
                    .textKy(optReq.getTextKy())
                    .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                    .orderIndex(optIndex++)
                    .build();
            optionRepository.save(option);
        }

        return toAdminQuestionResponse(questionRepository.findById(question.getId()).orElseThrow());
    }

    @Transactional
    public AdminQuestionResponse updateQuestion(Long questionId, CreateQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException("Вопрос не найден.", "Суроо табылган жок."));

        question.setText(request.getText());
        question.setTextKy(request.getTextKy());
        question.setSectionName(request.getSectionName());
        question.setSectionNameKy(request.getSectionNameKy());
        question.setImageUrl(request.getImageUrl());
        question.setExplanation(request.getExplanation());
        question.setExplanationKy(request.getExplanationKy());
        question.setOrderIndex(request.getOrderIndex());
        question.setPointValue(request.getPointValue());

        // Replace options
        optionRepository.deleteAll(
                optionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId));

        int optIndex = 0;
        for (CreateQuestionRequest.AnswerOptionRequest optReq : request.getOptions()) {
            AnswerOption option = AnswerOption.builder()
                    .question(question)
                    .label(optReq.getLabel())
                    .text(optReq.getText())
                    .textKy(optReq.getTextKy())
                    .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                    .orderIndex(optIndex++)
                    .build();
            optionRepository.save(option);
        }

        return toAdminQuestionResponse(questionRepository.findById(questionId).orElseThrow());
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException("Вопрос не найден.", "Суроо табылган жок."));
        question.setActive(false);
        questionRepository.save(question);
    }

    // ===== MAPPERS =====

    private AdminTestResponse toAdminTestResponse(Test test) {
        List<SubTest> subTests = subTestRepository.findByTestIdOrderByLevelOrderAsc(test.getId());
        return AdminTestResponse.builder()
                .id(test.getId())
                .title(test.getTitle())
                .titleKy(test.getTitleKy())
                .description(test.getDescription())
                .descriptionKy(test.getDescriptionKy())
                .iconUrl(test.getIconUrl())
                .price(test.getPrice())
                .active(test.getActive())
                .subTests(subTests.stream().map(this::toAdminSubTestResponse).toList())
                .build();
    }

    private AdminTestResponse.AdminSubTestResponse toAdminSubTestResponse(SubTest st) {
        return AdminTestResponse.AdminSubTestResponse.builder()
                .id(st.getId())
                .title(st.getTitle())
                .titleKy(st.getTitleKy())
                .levelName(st.getLevelName())
                .levelNameKy(st.getLevelNameKy())
                .levelOrder(st.getLevelOrder())
                .isPaid(st.getIsPaid())
                .durationMinutes(st.getDurationMinutes())
                .questionCount(questionRepository.countBySubTestIdAndActiveTrue(st.getId()))
                .active(st.getActive())
                .build();
    }

    private AdminQuestionResponse toAdminQuestionResponse(Question q) {
        List<AdminQuestionResponse.OptionResponse> options = optionRepository
                .findByQuestionIdOrderByOrderIndexAsc(q.getId()).stream()
                .map(o -> AdminQuestionResponse.OptionResponse.builder()
                        .id(o.getId())
                        .label(o.getLabel())
                        .text(o.getText())
                        .textKy(o.getTextKy())
                        .isCorrect(o.getIsCorrect())
                        .orderIndex(o.getOrderIndex())
                        .build())
                .toList();

        return AdminQuestionResponse.builder()
                .id(q.getId())
                .sectionName(q.getSectionName())
                .sectionNameKy(q.getSectionNameKy())
                .text(q.getText())
                .textKy(q.getTextKy())
                .imageUrl(q.getImageUrl())
                .explanation(q.getExplanation())
                .explanationKy(q.getExplanationKy())
                .orderIndex(q.getOrderIndex())
                .pointValue(q.getPointValue())
                .active(q.getActive())
                .options(options)
                .build();
    }
}