#version 150

uniform sampler2D Sampler0;

uniform float GameTime;

in vec2 texCoord0;
out vec4 fragColor;

vec3 rainbow(float angle) {
    return vec3(0.5 + cos(angle) * 0.5, 0.5 + cos(angle + 2.09439510239) * 0.5, 0.5 + cos(angle + 4.18879020479) * 0.5);
}

void main() {
    vec2 uv = texCoord0;

    vec4 mask = texture(Sampler0, texCoord0);
    if(mask.a <= 0.1)
        discard;

    uv -= 0.5;

    float angle = atan(uv.y, uv.x) + GameTime * 1000.;

    vec3 col = rainbow(angle);

    fragColor = vec4(col.rgb, mask.a);
}