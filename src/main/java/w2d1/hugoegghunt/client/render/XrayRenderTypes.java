package w2d1.hugoegghunt.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;

/**
 * Copies the vanilla {@link RenderPipelines#LINES} pipeline, switching its depth test
 * to {@link DepthTestFunction#NO_DEPTH_TEST} so tracers and boxes render through walls.
 */
public final class XrayRenderTypes {

    public static final RenderType LINES_NO_DEPTH_TEST = createLinesNoDepthTest();

    private XrayRenderTypes() {}

    private static RenderType createLinesNoDepthTest() {
        var base = RenderPipelines.LINES;

        RenderPipeline.Builder pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("hugoegghunt", "lines_no_depth_test"))
                .withVertexShader(base.getVertexShader())
                .withFragmentShader(base.getFragmentShader())
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withVertexFormat(base.getVertexFormat(), base.getVertexFormatMode());

        // Carry over shader defines from the vanilla pipeline
        var defines = base.getShaderDefines();
        for (var entry : defines.values().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            try {
                pipeline.withShaderDefine(key, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                try {
                    pipeline.withShaderDefine(key, Float.parseFloat(value));
                } catch (NumberFormatException e2) {
                    pipeline.withShaderDefine(key);
                }
            }
        }
        for (String flag : defines.flags()) {
            pipeline.withShaderDefine(flag);
        }

        // Carry over uniforms and samplers
        for (var uniform : base.getUniforms()) {
            if (uniform.type() != null && uniform.textureFormat() != null) {
                pipeline.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
            } else if (uniform.type() != null) {
                pipeline.withUniform(uniform.name(), uniform.type());
            }
        }
        for (String sampler : base.getSamplers()) {
            pipeline.withSampler(sampler);
        }

        base.getBlendFunction().ifPresent(pipeline::withBlend);

        RenderPipeline noDepthPipeline = pipeline.build();
        RenderSetup setup = RenderSetup.builder(noDepthPipeline)
                .setLayeringTransform(LayeringTransform.NO_LAYERING)
                .createRenderSetup();
        return RenderType.create("hugoegghunt_lines_no_depth_test", setup);
    }
}
