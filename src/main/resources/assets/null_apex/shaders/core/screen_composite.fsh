#version 150

uniform sampler2D SceneSampler;
uniform sampler2D MaskSampler;
uniform sampler2D DepthSampler;
uniform vec3 MaskColor;
uniform float MaskStrength;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 sceneColor = texture(SceneSampler, texCoord);
    float mask = clamp(texture(MaskSampler, texCoord).r * MaskStrength, 0.0, 1.0);
    float sceneDepth = clamp(texture(DepthSampler, texCoord).r, 0.0, 1.0);
    vec3 depthTint = mix(MaskColor, vec3(1.0) - MaskColor, sceneDepth * 0.2);
    fragColor = vec4(mix(sceneColor.rgb, depthTint, mask), sceneColor.a);
}
