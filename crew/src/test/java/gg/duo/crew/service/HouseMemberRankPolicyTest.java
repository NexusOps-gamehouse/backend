package gg.duo.crew.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HouseMemberRankPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, false",
            "2, 20, false",
            "3, 9, false",
            "3, 10, true",
            "4, 0, false",
            "5, 0, true",
            "5, 100, true"
    })
    void qualifiesByGameAndChatActivity(int gameCount, int chatCount, boolean expected) {
        assertEquals(expected,
                HouseMemberRankPolicy.qualifiesForRegularMember(gameCount, chatCount));
    }
}
