#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec3 normal;
layout (location=2) in vec2 texCoord;

out vec2 outTextCoord;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

void main()
{
    // Utiliser seulement la partie rotation de la vue
    mat4 viewWithoutTranslation = mat4(mat3(viewMatrix));
    
    // Position de la skybox (sans translation)
    vec4 pos = projectionMatrix * viewWithoutTranslation * modelMatrix * vec4(position, 1.0);
    
    // Forcer la profondeur au maximum (dernier plan)
    gl_Position = vec4(pos.xy, pos.w, pos.w);
    
    outTextCoord = texCoord;
}
