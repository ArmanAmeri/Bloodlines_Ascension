// TEST (Veil learning spike): recolor the sky to dark blood and the sun to
// bright blood. Sky pixels are the ones with nothing written to the depth
// buffer (depth == 1.0), so terrain/entities/clouds are untouched. Brightness
// of the original pixel picks the shade: night sky -> blood_black, day sky ->
// blood_dark, sun/stars -> blood_bright (canonical mod palette).

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler0, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;

    if (depth >= 0.9999) {
        float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));

        vec3 deep = vec3(0.051, 0.016, 0.024);   // blood_black
        vec3 dark = vec3(0.420, 0.059, 0.086);   // blood_dark
        vec3 bright = vec3(0.784, 0.125, 0.173); // blood_bright

        vec3 sky = mix(deep, dark, smoothstep(0.0, 0.70, lum));
        sky = mix(sky, bright, smoothstep(0.75, 0.95, lum));
        fragColor = vec4(sky, color.a);
    } else {
        fragColor = color;
    }
}
