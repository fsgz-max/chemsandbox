package com.example.chemsandbox

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SandRenderer(private val sim: Simulation) : GLSurfaceView.Renderer {

    private val pixels = IntArray(sim.width * sim.height)
    private val texBuf = ByteBuffer.allocateDirect(sim.width * sim.height * 4).order(ByteOrder.nativeOrder())
    private val texId = IntArray(1)
    private var program = 0
    private var aPos = 0; private var aUV = 0; private var uTex = 0

    private val quad: FloatBuffer by lazy {
        val data = floatArrayOf(
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        )
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply { put(data); position(0) }
    }

    private val VSH = "attribute vec2 aPos; attribute vec2 aUV; varying vec2 vUV; " +
        "void main(){ vUV = aUV; gl_Position = vec4(aPos, 0.0, 1.0); }"

    private val FSH = "precision mediump float; varying vec2 vUV; uniform sampler2D uTex; " +
        "void main(){ gl_FragColor = texture2D(uTex, vUV); }"

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = createProgram(loadShader(GL_VERTEX_SHADER, VSH), loadShader(GL_FRAGMENT_SHADER, FSH))
        aPos = glGetAttribLocation(program, "aPos")
        aUV  = glGetAttribLocation(program, "aUV")
        uTex = glGetUniformLocation(program, "uTex")

        glGenTextures(1, texId, 0)
        glBindTexture(GL_TEXTURE_2D, texId[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, sim.width, sim.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        sim.step()
        sim.render(pixels)
        uploadTexture()

        glClearColor(0.05f, 0.05f, 0.07f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        glUseProgram(program)
        glBindTexture(GL_TEXTURE_2D, texId[0])
        glUniform1i(uTex, 0)

        quad.position(0); glVertexAttribPointer(aPos, 2, GL_FLOAT, false, 16, quad); glEnableVertexAttribArray(aPos)
        quad.position(2); glVertexAttribPointer(aUV,  2, GL_FLOAT, false, 16, quad); glEnableVertexAttribArray(aUV)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun uploadTexture() {
        texBuf.clear()
        for (c in pixels) {
            texBuf.put(((c shr 16) and 0xFF).toByte())
            texBuf.put(((c shr 8) and 0xFF).toByte())
            texBuf.put((c and 0xFF).toByte())
            texBuf.put(0xFF.toByte())
        }
        texBuf.flip()
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, sim.width, sim.height, GL_RGBA, GL_UNSIGNED_BYTE, texBuf)
    }

    private fun loadShader(type: Int, src: String): Int {
        val s = glCreateShader(type); glShaderSource(s, src); glCompileShader(s); return s
    }
    private fun createProgram(vs: Int, fs: Int): Int {
        val p = glCreateProgram(); glAttachShader(p, vs); glAttachShader(p, fs); glLinkProgram(p); return p
    }
}
