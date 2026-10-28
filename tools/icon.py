import sys
PETAL="M54,27 L50.6,22.6 C44,20.6 35.5,23 31.5,31 C26.5,42 35.5,58 45.5,71.5 C49.5,77 52,81.5 54,87 C56,81.5 58.5,77 62.5,71.5 C72.5,58 81.5,42 76.5,31 C72.5,23 64,20.6 57.4,22.6 Z"
NOTE="M44.6,60 A5.6,4.3 -18 1,0 55.8,56.6 A5.6,4.3 -18 1,0 44.6,60 Z M53.4,35 L56.2,35 L56.2,57.6 L53.4,57.6 Z M56.2,35 C61,36.8 65,41.2 63.8,47.6 C62,43.4 59.2,42.4 56.2,41.9 Z"
def svg(size, bg=True, fg=True, note=True, rot=-12):
    defs=f'''<defs>
<radialGradient id="bg" cx="54" cy="50" r="62" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#241a52"/><stop offset=".55" stop-color="#130f33"/><stop offset="1" stop-color="#08071a"/></radialGradient>
<filter id="halo" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
<filter id="halo2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2.2"/></filter>
<filter id="soft" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="1.6"/></filter>
<filter id="nglow" x="-80%" y="-80%" width="260%" height="260%"><feGaussianBlur stdDeviation="2.4"/></filter>
<linearGradient id="pet" x1="54" y1="20" x2="54" y2="86" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff4f8"/><stop offset=".45" stop-color="#ffd2e3"/><stop offset="1" stop-color="#ff9cc3"/></linearGradient>
<radialGradient id="poison" cx="36" cy="66" r="24" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#7a1fd6" stop-opacity=".95"/><stop offset=".45" stop-color="#a24bf0" stop-opacity=".68"/><stop offset="1" stop-color="#c88cff" stop-opacity="0"/></radialGradient>
<linearGradient id="rb" x1="44" y1="35" x2="64" y2="64" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#ff4d6d"/><stop offset=".18" stop-color="#ffa53d"/><stop offset=".34" stop-color="#ffe45c"/><stop offset=".5" stop-color="#5dfc8d"/><stop offset=".66" stop-color="#45d4ff"/><stop offset=".82" stop-color="#6b7bff"/><stop offset="1" stop-color="#d36bff"/></linearGradient>
<clipPath id="pc"><path d="{PETAL}"/></clipPath>
</defs>'''
    body=''
    if bg:
        body+='<rect width="108" height="108" fill="url(#bg)"/>'
        for x,y,r,o in [(20,24,.7,.7),(86,20,.9,.9),(92,62,.6,.6),(16,78,.8,.6),(30,94,.5,.5),(80,92,.6,.7),(64,12,.5,.5),(10,48,.5,.5)]:
            body+=f'<circle cx="{x}" cy="{y}" r="{r}" fill="#fff" opacity="{o}"/>'
    if fg:
        g=f'<g transform="rotate({rot} 54 54)">'
        g+=f'<path d="{PETAL}" fill="#ffffff" opacity=".85" filter="url(#halo)"/>'
        g+=f'<path d="{PETAL}" fill="#ffffff" opacity=".9" filter="url(#halo2)"/>'
        g+=f'<path d="{PETAL}" fill="url(#pet)"/>'
        # poison: blotch + veins + specks, clipped to petal
        g+='<g clip-path="url(#pc)">'
        g+='<ellipse cx="37" cy="64" rx="22" ry="18" fill="url(#poison)" filter="url(#soft)"/>'
        veins="M47,74 C43,66 38,58 33,50 M41,62 C38,60 35,59 32,59 M44,68 C41,68 38,69 36,71 M38,55 C37,52 36,50 34,47"
        g+=f'<path d="{veins}" stroke="#5a12a8" stroke-width=".7" fill="none" opacity=".6" stroke-linecap="round"/>'
        for x,y,r in [(35,57,1),(40,66,.8),(33,64,.6),(44,71,.5),(37,51,.45)]:
            g+=f'<circle cx="{x}" cy="{y}" r="{r}" fill="#4b0d93" opacity=".8"/>'
        g+='</g>'
        g+='<g clip-path="url(#pc)" stroke="#ffffff" stroke-width=".5" fill="none" opacity=".45" stroke-linecap="round"><path d="M54,84 C52,64 46,46 40,30"/><path d="M54,84 C54,62 54,44 54,30"/><path d="M54,84 C56,64 62,46 68,30"/><path d="M54,84 C50,68 41,54 34,42"/><path d="M54,84 C58,68 67,54 74,42"/></g>'
        g+=f'<path d="{PETAL}" fill="none" stroke="#ffffff" stroke-width=".8" opacity=".9"/>'
        if note:
            g+=f'<path d="{NOTE}" fill="url(#rb)" filter="url(#nglow)" opacity=".95"/>'
            g+=f'<path d="{NOTE}" fill="url(#rb)" stroke="#ffffff" stroke-width=".6"/>'
        g+='</g>'
        body+=g
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 108 108">{defs}{body}</svg>'
if __name__=='__main__':
    open('full.svg','w').write(svg(432))
    open('fg.svg','w').write(svg(432,bg=False))
    open('fg_nonote.svg','w').write(svg(432,bg=False,note=False))
    open('bg.svg','w').write(svg(432,fg=False))
