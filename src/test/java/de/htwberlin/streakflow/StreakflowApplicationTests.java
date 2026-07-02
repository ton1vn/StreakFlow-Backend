package de.htwberlin.streakflow;

import de.htwberlin.streakflow.model.Exercise;
import de.htwberlin.streakflow.model.ExerciseExecution;
import de.htwberlin.streakflow.repository.ExerciseExecutionRepository;
import de.htwberlin.streakflow.repository.ExerciseRepository;
import de.htwberlin.streakflow.repository.PlayerRepository;
import de.htwberlin.streakflow.repository.ShopPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private PlayerRepository playerRepository;

    private Exercise jogging;
    private Exercise strength;
    private Exercise yoga;

    @BeforeEach
    void setUp() {
        playerRepository.deleteAll();
        shopPurchaseRepository.deleteAll();
        executionRepository.deleteAll();
        exerciseRepository.deleteAll();

        jogging = exerciseRepository.save(new Exercise(null, "Joggen", "Cardio", 30));
        strength = exerciseRepository.save(new Exercise(null, "Krafttraining", "Strength", 45));
        yoga = exerciseRepository.save(new Exercise(null, "Yoga", "Mobility", 20));
    }

    @Test
    void createsExerciseAndShowsItInExerciseList() throws Exception {
        mockMvc.perform(post("/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Muay Thai\",\"category\":\"Kampfsport\",\"targetMinutesPerDay\":90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Muay Thai"));

        mockMvc.perform(get("/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Muay Thai")));
    }

    @Test
    void deletesExerciseAndItsExecutions() throws Exception {
        Exercise boxing = exerciseRepository.save(new Exercise(null, "Boxen", "Kampfsport", 60));
        executionRepository.save(new ExerciseExecution(null, LocalDate.now(), 60, boxing.getId(), "Boxen", 600, 6));

        mockMvc.perform(delete("/exercises/" + boxing.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.not(hasItem("Boxen"))));

        mockMvc.perform(get("/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void completesWorkoutAndUpdatesProgress() throws Exception {
        mockMvc.perform(post("/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":" + jogging.getId() + ",\"duration\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseName").value("Joggen"))
                .andExpect(jsonPath("$.earnedXp").value(300))
                .andExpect(jsonPath("$.earnedCoins").value(3));

        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(300))
                .andExpect(jsonPath("$.coins").value(3))
                .andExpect(jsonPath("$.completedToday").value(1));
    }

    @Test
    void preventsCompletingSameExerciseTwiceOnOneDay() throws Exception {
        mockMvc.perform(post("/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":" + jogging.getId() + ",\"duration\":30}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":" + jogging.getId() + ",\"duration\":30}"))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsCompletingSameExerciseAgainOnNextDay() throws Exception {
        executionRepository.save(new ExerciseExecution(null, LocalDate.now().minusDays(1), 30, jogging.getId(), "Joggen", 300, 3));

        mockMvc.perform(post("/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":" + jogging.getId() + ",\"duration\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exerciseName").value("Joggen"));
    }

    @Test
    void buysXpBoostAndReducesCoins() throws Exception {
        earnCoins(40);

        mockMvc.perform(post("/shop/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"xp-boost\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").value("xp-boost"))
                .andExpect(jsonPath("$.cost").value(30));

        mockMvc.perform(get("/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins").value(10))
                .andExpect(jsonPath("$.availableXpBoosts").value(1));
    }

    @Test
    void usesXpBoostAndDoublesNextWorkoutXp() throws Exception {
        earnCoins(40);
        String purchaseJson = mockMvc.perform(post("/shop/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"xp-boost\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String purchaseId = purchaseJson.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/shop/purchases/" + purchaseId + "/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedAt").exists());

        mockMvc.perform(post("/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":" + strength.getId() + ",\"duration\":45}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.earnedXp").value(900));
    }

    @Test
    void buysStreakFreezerAndReducesCoins() throws Exception {
        earnCoins(60);

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
    void rejectsShopPurchaseWhenCoinsAreMissing() throws Exception {
        mockMvc.perform(post("/shop/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"streak-freeze\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void addsPlayersButRejectsFifthPlayer() throws Exception {
        createPlayer("Toni");
        createPlayer("Linh");
        createPlayer("Mina");
        createPlayer("Sam");

        mockMvc.perform(get("/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].name", hasItem("Toni")));

        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Extra\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void completesAndDeletesPlayerWorkout() throws Exception {
        String playerJson = createPlayer("Toni");
        String playerId = playerJson.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/players/" + playerId + "/workouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutes").value(90))
                .andExpect(jsonPath("$.xp").value(900))
                .andExpect(jsonPath("$.coins").value(9));

        mockMvc.perform(delete("/players/" + playerId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private void earnCoins(int coins) {
        executionRepository.save(new ExerciseExecution(null, LocalDate.now().minusDays(1), coins * 10, yoga.getId(), "Yoga", coins * 100, coins));
    }

    private String createPlayer(String name) throws Exception {
        return mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
