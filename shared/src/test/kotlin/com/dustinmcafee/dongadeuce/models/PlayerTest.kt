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

    @Test
    fun `takeDamage to exactly 0 life causes loss`() {
        val player = Player(id = "1", name = "Test Player", life = 10)

        val defeated = player.takeDamage(10)

        assertEquals(0, defeated.life)
        assertTrue(defeated.hasLost, "Player should lose when life reaches 0")
    }

    @Test
    fun `takeDamage below 0 life causes loss`() {
        val player = Player(id = "1", name = "Test Player", life = 5)

        val defeated = player.takeDamage(10)

        assertEquals(-5, defeated.life)
        assertTrue(defeated.hasLost, "Player should lose when life goes below 0")
    }

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

    @Test
    fun `setLife to 0 causes loss`() {
        val player = Player(id = "1", name = "Test Player", life = 40)

        val defeated = player.setLife(0)

        assertEquals(0, defeated.life)
        assertTrue(defeated.hasLost, "Player should lose when life is set to 0")
    }

    @Test
    fun `setLife to negative value causes loss`() {
        val player = Player(id = "1", name = "Test Player", life = 40)

        val defeated = player.setLife(-5)

        assertEquals(-5, defeated.life)
        assertTrue(defeated.hasLost, "Player should lose when life is set to negative")
    }

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

    @Test
    fun `takeCommanderDamage of exactly 21 causes loss`() {
        val player = Player(id = "1", name = "Test Player")
        val commanderId = "cmd-123"

        val defeated = player.takeCommanderDamage(commanderId, 21)

        assertEquals(21, defeated.commanderDamage[commanderId])
        assertTrue(defeated.hasLost, "Player should lose at 21 commander damage")
    }

    @Test
    fun `takeCommanderDamage over 21 causes loss`() {
        val player = Player(id = "1", name = "Test Player")
        val commanderId = "cmd-123"

        val damaged = player.takeCommanderDamage(commanderId, 15)
        val defeated = damaged.takeCommanderDamage(commanderId, 10)

        assertEquals(25, defeated.commanderDamage[commanderId])
        assertTrue(defeated.hasLost, "Player should lose at 25 commander damage")
    }

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

    @Test
    fun `hasLost persists once set`() {
        val player = Player(id = "1", name = "Test Player", life = 5)

        val defeated = player.takeDamage(10)
        assertTrue(defeated.hasLost)

        // Even if they gain life, they stay defeated
        val stillDefeated = defeated.gainLife(50)
        assertTrue(stillDefeated.hasLost, "Loss state should persist")
    }

    @Test
    fun `multiple loss conditions dont override each other`() {
        val player = Player(id = "1", name = "Test Player", life = 1)
        val commanderId = "cmd-123"

        // Take lethal commander damage
        val commanderDefeated = player.takeCommanderDamage(commanderId, 21)
        assertTrue(commanderDefeated.hasLost)

        // Then take life damage
        val doubleDefeated = commanderDefeated.takeDamage(5)
        assertTrue(doubleDefeated.hasLost, "Should stay defeated")
        assertEquals(-4, doubleDefeated.life)
    }
}
