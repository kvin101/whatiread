package com.whatiread.goal.service;

import com.whatiread.goal.api.ReadingGoalDto;
import com.whatiread.goal.api.UpsertReadingGoalRequest;
import java.util.UUID;

public interface ReadingGoalService {

    ReadingGoalDto get(UUID userId, short year);

    ReadingGoalDto upsert(UUID userId, short year, UpsertReadingGoalRequest request);
}
