// TEST (Veil learning spike): blood-world grade. Sky pixels (nothing in the
// depth buffer) become black blood with a bright blood sun; world pixels get
// a moody grade — darkened, greens muted, pulled toward the blood palette —
// so terrain sits in the same eternal-night mood as the sky.

// World grade tuning
#define WORLD_DARKEN 0.62
#define WORLD_DESAT 0.40
#define WORLD_TINT vec3(1.00, 0.66, 0.70)
// How much bright sky shows a dark-red tinge (0 = pure black sky)
#define SKY_TINGE 0.45

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler0, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    vec3 deep = vec3(0.051, 0.016, 0.024);   // blood_black
    vec3 dark = vec3(0.420, 0.059, 0.086);   // blood_dark
    vec3 bright = vec3(0.784, 0.125, 0.173); // blood_bright

    if (depth >= 0.9999) {
        // Black-blood sky: blood_black dominates day and night, with only a
        // subtle dark-red tinge where the sky is bright; the sun burns bright
        vec3 sky = mix(deep, dark, smoothstep(0.45, 0.95, lum) * SKY_TINGE);
        sky = mix(sky, bright, smoothstep(0.82, 0.96, lum));
        fragColor = vec4(sky, color.a);
    } else {
        // World: darken, mute the greens, keep reds alive, drift toward blood
        vec3 graded = mix(color.rgb, vec3(lum), WORLD_DESAT) * WORLD_TINT * WORLD_DARKEN;
        graded = mix(graded, dark * (0.4 + lum), 0.12);
        fragColor = vec4(graded, color.a);
    }
}
