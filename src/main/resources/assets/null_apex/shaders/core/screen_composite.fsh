#version 150

uniform sampler2D SceneSampler;
uniform sampler2D MaskSampler;
uniform sampler2D DepthSampler;
uniform float Operation;
uniform vec3 MaskColor;
uniform float MaskStrength;
uniform float WaveAmplitudePixels;
uniform float WaveFrequency;
uniform float WaveSpeed;
uniform float WaveTime;
uniform float ViewportWidth;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 sceneColor = texture(SceneSampler, texCoord);
    float mask = clamp(texture(MaskSampler, texCoord).r * MaskStrength, 0.0, 1.0);
    if (Operation < 0.5) {
        float sceneDepth = clamp(texture(DepthSampler, texCoord).r, 0.0, 1.0);
        vec3 depthTint = mix(MaskColor, vec3(1.0) - MaskColor, sceneDepth * 0.2);
        fragColor = vec4(mix(sceneColor.rgb, depthTint, mask), sceneColor.a);
        return;
    }

    float phase = (texCoord.y * WaveFrequency + WaveTime * WaveSpeed) * 6.2831853;
    float horizontalOffset = sin(phase) * WaveAmplitudePixels / max(ViewportWidth, 1.0);
    vec2 displacedUv = clamp(texCoord + vec2(horizontalOffset, 0.0), vec2(0.0), vec2(1.0));
    vec4 distortedColor = texture(SceneSampler, displacedUv);
    fragColor = vec4(mix(sceneColor.rgb, distortedColor.rgb, mask), sceneColor.a);
}
