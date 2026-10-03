#version 150

#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
flat in vec2 effectData;

out vec4 fragColor;

void main() {
    float progress = clamp(effectData.x, 0.0, 1.0);
    float seed = clamp(effectData.y, 0.0, 1.0);
    vec3 color;
    float alpha;

    if (max(max(vertexColor.r, vertexColor.g), vertexColor.b) < 0.08) {
        float latitudeShade = sin(texCoord0.y * 3.14159265);
        color = vertexColor.rgb * ColorModulator.rgb
            + vec3(0.003, 0.0005, 0.009) * latitudeShade * latitudeShade;
        alpha = vertexColor.a * ColorModulator.a;
    } else {
        vec2 centeredUv = (texCoord0 - vec2(0.5)) * 2.0;
        float radius = length(centeredUv);
        float angle = atan(centeredUv.y, centeredUv.x);
        float innerEdge = exp(-abs(radius - 0.43) * 58.0);
        float outerEdge = exp(-abs(radius - 0.91) * 24.0);
        float edgeFade = smoothstep(0.37, 0.44, radius) * (1.0 - smoothstep(0.94, 1.0, radius));
        float turbulence = 0.84 + 0.16 * sin(angle * 23.0 + radius * 37.0 + progress * 19.0 + seed * 6.2831853);
        float filaments = 0.90 + 0.10 * sin(angle * 47.0 - radius * 22.0 + seed * 31.0);

        vec3 blue = vec3(0.025, 0.18, 0.72);
        vec3 violet = vec3(0.40, 0.035, 0.78);
        vec3 hot = vec3(1.0, 0.58, 0.96);
        color = mix(blue, violet, smoothstep(0.57, 0.98, radius));
        color = mix(color, hot, clamp(innerEdge * 0.95, 0.0, 1.0));
        color = mix(color, vec3(0.32, 0.58, 1.0), clamp(outerEdge * 0.48, 0.0, 1.0));
        color *= turbulence * filaments * vertexColor.rgb * ColorModulator.rgb;
        alpha = vertexColor.a * ColorModulator.a * edgeFade * (0.82 + 0.18 * turbulence);
    }

    fragColor = linear_fog(vec4(color, alpha), vertexDistance, FogStart, FogEnd, FogColor);
}
