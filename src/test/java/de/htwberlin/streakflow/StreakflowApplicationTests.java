package de.htwberlin.streakflow;

import de.htwberlin.streakflow.controller.ExerciseController;
import de.htwberlin.streakflow.model.Exercise;
import de.htwberlin.streakflow.model.ExerciseExecution;
import de.htwberlin.streakflow.model.UserProgress;
import de.htwberlin.streakflow.repository.ExerciseExecutionRepository;
import de.htwberlin.streakflow.repository.ExerciseRepository;
import de.htwberlin.streakflow.repository.ShopPurchaseRepository;
import de.htwberlin.streakflow.service.StreakFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StreakflowApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ExerciseExecutionRepository executionRepository;

    @Autowired
    private ShopPurchaseRepository shopPurchaseRepository;

    private Exercise jogging;

    @BeforeEach
    void setUp() {
        shopPurchaseRepository.deleteAll();
        executionRepository.deleteAll();
        exerciseRepository.deleteAll();

        jogging = exerciseRepository.save(new Exercise(null, "Joggen", "Cardio", 30));
        exerciseRepository.save(new Exercise(null, "Krafttraining", "Strength", 45));
        exerciseRepository.save(new Exercise(null, "Yoga", "Mobility", 20));
    }

    @Test
    void getExercisesReturns200() throws Exception {
        mockMvc.perform(get("/exercises"))
                .andExpect(status().isOk());
    }

    @Test
    void getExercisesReturnsList() throws Exception {
        mockMvc.perform(get("/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getExercisesContainsJoggen() throws Exception {
        mockMvc.perform(get("/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Joggen")));
    }

    @Test
    void getExecutionsReturns200() throws Exception {
        mockMvc.perform(get("/executions"))
                .andExpect(status().isOk());
    }

    @Test
    void getExecutionsReturnsList() throws Exception {
        executionRepository.save(new ExerciseExecution(null, LocalDate.now(), 30, jogging.getId(), "Joggen", 300, 3));

        mockMvc.perform(get("/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getProgressReturns200() throws Exception {
        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk());
    }

    @Test
    void getProgressContainsCurrentStreak() throws Exception {
        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").exists());
    }

    @Test
    void getProgressContainsXp() throws Exception {
        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").exists());
    }

    @Test
    void postExecutionsCreatesExecution() throws Exception {
        mockMvc.perform(post("/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":" + jogging.getId() + ",\"duration\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseName").value("Joggen"))
                .andExpect(jsonPath("$.earnedXp").value(300));

        mockMvc.perform(get("/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void postShopFreezerReducesCoinsAndIncreasesFreezer() throws Exception {
        executionRepository.save(new ExerciseExecution(null, LocalDate.now(), 600, jogging.getId(), "Joggen", 6000, 60));

        mockMvc.perform(post("/shop/freezer"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value("streak-freeze"))
                .andExpect(jsonPath("$.cost").value(50));

        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(10))
                .andExpect(jsonPath("$.streakFreezers").value(1));
    }

    @Test
    void controllerReturnsExerciseFallbackWhenServiceFails() {
        StreakFlowService service = mock(StreakFlowService.class);
        Exercise fallbackExercise = new Exercise(1L, "Joggen", "Cardio", 30);
        when(service.getExercises()).thenThrow(new RuntimeException("Database unavailable"));
        when(service.fallbackExercises()).thenReturn(List.of(fallbackExercise));

        ExerciseController controller = new ExerciseController(service);

        assertEquals("Joggen", controller.getExercises().getFirst().getName());
    }

    @Test
    void controllerReturnsProgressFallbackWhenServiceFails() {
        StreakFlowService service = mock(StreakFlowService.class);
        UserProgress fallbackProgress = new UserProgress(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, null);
        when(service.getProgress()).thenThrow(new RuntimeException("Database unavailable"));
        when(service.fallbackProgress()).thenReturn(fallbackProgress);

        ExerciseController controller = new ExerciseController(service);

        assertEquals(0, controller.getProgress().getXp());
    }
}
