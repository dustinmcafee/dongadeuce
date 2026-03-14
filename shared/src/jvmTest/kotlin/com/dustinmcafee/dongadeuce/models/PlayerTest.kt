package com.dustinmcafee.dongadeuce.models

import kotlin.test.*

class PlayerTest {

    @Test
    fun `player starts with 40 life and no losses`() {
        val player = Player(id = "1", name = "Test Player")

        assertEquals(40, player.life)
        assertFalse(player.hasLost)
    }

    @Test
    fun `takeDamage reduces life total`() {
        val player = Player(id = "1", name = "Test Player", life = 40)

        val damaged = player.takeDamage(5)

        assertEquals(35, damaged.life)
        assertFalse(damaged.hasLost)
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `takeDamage to exactly 0 life causes loss`() {
    //     val player = Player(id = "1", name = "Test Player", life = 10)
    //     val defeated = player.takeDamage(10)
    //     assertEquals(0, defeated.life)
    //     assertTrue(defeated.hasLost, "Player should lose when life reaches 0")
    // }

    // @Test
    // fun `takeDamage below 0 life causes loss`() {
    //     val player = Player(id = "1", name = "Test Player", life = 5)
    //     val defeated = player.takeDamage(10)
    //     assertEquals(-5, defeated.life)
    //     assertTrue(defeated.hasLost, "Player should lose when life goes below 0")
    // }

    @Test
    fun `gainLife increases life total`() {
        val player = Player(id = "1", name = "Test Player", life = 30)

        val healed = player.gainLife(10)

        assertEquals(40, healed.life)
        assertFalse(healed.hasLost)
    }

    @Test
    fun `setLife to positive value does not cause loss`() {
        val player = Player(id = "1", name = "Test Player", life = 40)

        val updated = player.setLife(20)

        assertEquals(20, updated.life)
        assertFalse(updated.hasLost)
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `setLife to 0 causes loss`() {
    //     val player = Player(id = "1", name = "Test Player", life = 40)
    //     val defeated = player.setLife(0)
    //     assertEquals(0, defeated.life)
    //     assertTrue(defeated.hasLost, "Player should lose when life is set to 0")
    // }

    // @Test
    // fun `setLife to negative value causes loss`() {
    //     val player = Player(id = "1", name = "Test Player", life = 40)
    //     val defeated = player.setLife(-5)
    //     assertEquals(-5, defeated.life)
    //     assertTrue(defeated.hasLost, "Player should lose when life is set to negative")
    // }

    @Test
    fun `takeCommanderDamage tracks damage per commander`() {
        val player = Player(id = "1", name = "Test Player")
        val commanderId = "cmd-123"

        val damaged = player.takeCommanderDamage(commanderId, 5)

        assertEquals(5, damaged.commanderDamage[commanderId])
        assertFalse(damaged.hasLost)
    }

    @Test
    fun `takeCommanderDamage accumulates over multiple hits`() {
        val player = Player(id = "1", name = "Test Player")
        val commanderId = "cmd-123"

        val damage1 = player.takeCommanderDamage(commanderId, 10)
        val damage2 = damage1.takeCommanderDamage(commanderId, 7)

        assertEquals(17, damage2.commanderDamage[commanderId])
        assertFalse(damage2.hasLost)
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `takeCommanderDamage of exactly 21 causes loss`() {
    //     val player = Player(id = "1", name = "Test Player")
    //     val commanderId = "cmd-123"
    //     val defeated = player.takeCommanderDamage(commanderId, 21)
    //     assertEquals(21, defeated.commanderDamage[commanderId])
    //     assertTrue(defeated.hasLost, "Player should lose at 21 commander damage")
    // }

    // @Test
    // fun `takeCommanderDamage over 21 causes loss`() {
    //     val player = Player(id = "1", name = "Test Player")
    //     val commanderId = "cmd-123"
    //     val damaged = player.takeCommanderDamage(commanderId, 15)
    //     val defeated = damaged.takeCommanderDamage(commanderId, 10)
    //     assertEquals(25, defeated.commanderDamage[commanderId])
    //     assertTrue(defeated.hasLost, "Player should lose at 25 commander damage")
    // }

    @Test
    fun `commander damage tracked separately per commander`() {
        val player = Player(id = "1", name = "Test Player")
        val commander1 = "cmd-1"
        val commander2 = "cmd-2"

        val damage1 = player.takeCommanderDamage(commander1, 15)
        val damage2 = damage1.takeCommanderDamage(commander2, 10)

        assertEquals(15, damage2.commanderDamage[commander1])
        assertEquals(10, damage2.commanderDamage[commander2])
        assertFalse(damage2.hasLost, "Should not lose with sub-lethal damage from multiple commanders")
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `hasLost persists once set`() {
    //     val player = Player(id = "1", name = "Test Player", life = 5)
    //
    //     val defeated = player.takeDamage(10)
    //     assertTrue(defeated.hasLost)
    //
    //     // Even if they gain life, they stay defeated
    //     val stillDefeated = defeated.gainLife(50)
    //     assertTrue(stillDefeated.hasLost, "Loss state should persist")
    // }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `multiple loss conditions dont override each other`() {
    //     val player = Player(id = "1", name = "Test Player", life = 1)
    //     val commanderId = "cmd-123"
    //
    //     // Take lethal commander damage
    //     val commanderDefeated = player.takeCommanderDamage(commanderId, 21)
    //     assertTrue(commanderDefeated.hasLost)
    //
    //     // Then take life damage
    //     val doubleDefeated = commanderDefeated.takeDamage(5)
    //     assertTrue(doubleDefeated.hasLost, "Should stay defeated")
    //     assertEquals(-4, doubleDefeated.life)
    // }

    // Counter Tests

    @Test
    fun `addCounter adds poison counters`() {
        val player = Player(id = "1", name = "Test Player")

        val poisoned = player.addCounter("poison", 3)

        assertEquals(3, poisoned.getCounter("poison"), "Should have 3 poison counters")
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `addCounter at 10 poison causes loss`() {
    //     val player = Player(id = "1", name = "Test Player")
    //
    //     val poisoned = player.addCounter("poison", 10)
    //
    //     assertEquals(10, poisoned.getCounter("poison"))
    //     assertTrue(poisoned.hasLost, "Player should lose at 10 poison counters")
    // }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `addCounter above 10 poison causes loss`() {
    //     val player = Player(id = "1", name = "Test Player")
    //         .addCounter("poison", 5)
    //
    //     val morePoisoned = player.addCounter("poison", 7)
    //
    //     assertEquals(12, morePoisoned.getCounter("poison"))
    //     assertTrue(morePoisoned.hasLost, "Player should lose above 10 poison counters")
    // }

    @Test
    fun `addCounter 9 poison does not cause loss`() {
        val player = Player(id = "1", name = "Test Player")

        val poisoned = player.addCounter("poison", 9)

        assertEquals(9, poisoned.getCounter("poison"))
        assertFalse(poisoned.hasLost, "Player should not lose at 9 poison counters")
    }

    @Test
    fun `removeCounter removes poison`() {
        val player = Player(id = "1", name = "Test Player")
            .addCounter("poison", 5)

        val lessPoison = player.removeCounter("poison", 2)

        assertEquals(3, lessPoison.getCounter("poison"), "Should have 3 poison counters")
    }

    @Test
    fun `removeCounter cannot go below zero`() {
        val player = Player(id = "1", name = "Test Player")
            .addCounter("poison", 3)

        val removed = player.removeCounter("poison", 10)

        assertEquals(0, removed.getCounter("poison"), "Poison should not go below 0")
    }

    @Test
    fun `setCounter sets exact value`() {
        val player = Player(id = "1", name = "Test Player")
            .addCounter("poison", 3)

        val set = player.setCounter("poison", 7)

        assertEquals(7, set.getCounter("poison"), "Should have exactly 7 poison counters")
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `setCounter at 10 poison causes loss`() {
    //     val player = Player(id = "1", name = "Test Player")
    //
    //     val poisoned = player.setCounter("poison", 10)
    //
    //     assertEquals(10, poisoned.getCounter("poison"))
    //     assertTrue(poisoned.hasLost, "Player should lose when poison set to 10")
    // }

    @Test
    fun `getCounter returns 0 for missing type`() {
        val player = Player(id = "1", name = "Test Player")

        val count = player.getCounter("nonexistent")

        assertEquals(0, count, "Missing counter type should return 0")
    }

    @Test
    fun `energy counters have no threshold`() {
        val player = Player(id = "1", name = "Test Player")

        val energized = player.addCounter("energy", 100)

        assertEquals(100, energized.getCounter("energy"))
        assertFalse(energized.hasLost, "Energy counters should not cause loss")
    }

    @Test
    fun `experience counters have no threshold`() {
        val player = Player(id = "1", name = "Test Player")

        val experienced = player.addCounter("experience", 50)

        assertEquals(50, experienced.getCounter("experience"))
        assertFalse(experienced.hasLost, "Experience counters should not cause loss")
    }

    @Test
    fun `multiple counter types tracked independently`() {
        val player = Player(id = "1", name = "Test Player")
            .addCounter("poison", 5)
            .addCounter("energy", 10)
            .addCounter("experience", 3)

        assertEquals(5, player.getCounter("poison"))
        assertEquals(10, player.getCounter("energy"))
        assertEquals(3, player.getCounter("experience"))
    }

    @Test
    fun `removing all counters removes counter type from map`() {
        val player = Player(id = "1", name = "Test Player")
            .addCounter("poison", 3)

        val removed = player.removeCounter("poison", 3)

        assertEquals(0, removed.getCounter("poison"))
        assertFalse(removed.counters.containsKey("poison"), "Counter type should be removed from map")
    }
}
