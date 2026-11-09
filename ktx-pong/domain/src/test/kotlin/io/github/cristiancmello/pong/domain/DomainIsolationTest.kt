package io.github.cristiancmello.pong.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Guards EN-02: the domain module must stay free of libGDX so the game can
 * be simulated without a screen.
 */
class DomainIsolationTest {
    @Test
    fun `libGDX is not on the domain classpath`() {
        assertFailsWith<ClassNotFoundException> {
            Class.forName("com.badlogic.gdx.Game")
        }
    }
}
