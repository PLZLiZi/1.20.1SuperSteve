#version 330

uniform float GameTime;
in vec2 texCoord0;
out vec4 fragColor;

#define RANDOM_INVERT
//#define FLAT_TOP

const vec2 s = vec2(1, 1.732);

const vec2[6] vID = vec2[6](vec2(-6, -2), vec2(-6, 2), vec2(0, 4), vec2(6, 2), vec2(6, -2), vec2(0, -4));

const vec2[6] eID = vec2[6](vec2(-6, 0), vec2(-3, 3), vec2(3, 3), vec2(6, 0), vec2(3, -3), vec2(-3, -3));

float hash21(vec2 f) {
    uvec2 p = floatBitsToUint(f);
    p = 1664525U * (p >> 1U ^ p.yx);
    return float(1103515245U * (p.x ^ (p.y >> 3U))) / float(0xffffffffU);
}

float distLineS(vec2 p, vec2 a, vec2 b) {
    b -= a;
    return dot(p - a, vec2(-b.y, b.x) / length(b));
}

float getHex(vec2 p) {
    return max(dot(abs(p.xy), s / 2.), abs(p.x * s.x));
}

vec4 getGrid(vec2 p) {
    vec4 ip = floor(vec4(p / s, p / s - .5));
    vec4 q = p.xyxy - vec4(ip.xy + .5, ip.zw + 1.) * s.xyxy;
    return dot(q.xy, q.xy) < dot(q.zw, q.zw) ? vec4(q.xy, ip.xy * 12.) : vec4(q.zw, ip.zw * 12. + 6.);
}

void main() {
    vec2 uv = (texCoord0 - 0.5) * 2.0;

    const float sc = 8.0;
    float sf = 0.005;

    float time = GameTime * 1000;

    vec2 camDir = normalize(s);
    vec2 p = uv * sc + camDir * time / 3.0;

    vec4 p4 = getGrid(p);
    vec2 sDiv12 = s / 12.;

    #ifdef RANDOM_INVERT
    float rndT = hash21(p4.zw + .01) < .5 ? -1. : 1.;
    if(rndT < 0.)
        p4.y = -p4.y;
    #endif

    float vLn[6];
    float hexShape = getHex(p4.xy) - .5;

    for(int i = 0; i < 6; i++) {
        vLn[i] = distLineS(p4.xy, vec2(0), vID[i] * sDiv12);
    }

    vec3 cube;
    cube.x = max(max(hexShape, vLn[1]), -vLn[3]);
    cube.y = max(max(hexShape, vLn[3]), -vLn[5]);
    cube.z = max(max(hexShape, vLn[5]), -vLn[1]);

    vec3 col = vec3(0);
    vec3 shade = vec3(.9, .5, .4);

    vec3 shad = vec3(1e5);
    shad.x = max(cube.x, distLineS(p4.xy, vID[3] * sDiv12, eID[1] * sDiv12));
    shad.y = max(cube.y, distLineS(p4.xy, eID[4] * sDiv12, vID[3] * sDiv12));
    shad.z = cube.z;
    #ifdef RANDOM_INVERT
    if(rndT < 0.) {
        shad.xy = cube.xy;
    }
    if(rndT < 0.)
        shade = shade.xzy;
    #endif

    for(int i = 0; i < 3; i++) {
        float hue = (p.x + p.y * 0.5) * 0.12;

        vec3 rainbow = 0.5 + 0.5 * cos(6.2831853 * (hue + vec3(0.0, 0.33, 0.67)));
        rainbow = pow(rainbow, vec3(0.8)) * 1.5;

        vec3 faceCol = rainbow;
        vec3 edgeCol = faceCol / 8.0;
        vec3 trimCol = vec3(1.5, 1.2, 1.0) * mix(faceCol, vec3(1.0), .4);

        edgeCol *= shade[i];
        trimCol *= shade[i];
        faceCol *= shade[i];

        col = mix(col, edgeCol, (1. - smoothstep(0., sf, cube[i])));
        col = mix(col, trimCol, (1. - smoothstep(0., sf, cube[i] + 1. / 56.)));
        col = mix(col, edgeCol, (1. - smoothstep(0., sf, cube[i] + 1. / 56. + 1. / 28.)));
        col = mix(col, faceCol, (1. - smoothstep(0., sf, cube[i] + 2.5 / 56. + 1. / 28.)));
    }

    for(int i = 0; i < 3; i++) {
        col = mix(col, col * .5, (1. - smoothstep(0., 0.02, shad[i] + .015)));
    }

    #ifdef RANDOM_INVERT
    if(rndT > 0.)
        col *= max(1. - length(p4.xy) * .95, 0.);
    else
        col *= max(.25 + length(p4.xy) * .75, 0.);
    #else
    col *= max(1. - length(p4.xy) * .95, 0.);
    #endif

    col = mix(col, vec3(1.), 0.03);

    fragColor = vec4(pow(max(col, 0.), vec3(1. / 2.2)), 1);
}