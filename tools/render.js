const {chromium}=require('playwright');const fs=require('fs');
(async()=>{const b=await chromium.launch();const p=await b.newPage();
 for(const [name,size] of JSON.parse(process.argv[2])){const svg=fs.readFileSync(name+'.svg','utf8').replace(/width="432" height="432"/,`width="${size}" height="${size}"`);
  await p.setViewportSize({width:size,height:size});await p.setContent(`<body style="margin:0;background:transparent">${svg}</body>`);
  await p.screenshot({path:`${name}_${size}.png`,omitBackground:true,clip:{x:0,y:0,width:size,height:size}})}
 await b.close()})();
