import * as THREE from 'three';
import {OrbitControls} from './vendor/OrbitControls.js';
const $=id=>document.getElementById(id);
const log=m=>{const o=$('out');o.textContent+=m+'\n';o.scrollTop=1e9};

// ---- data model ----
let root, sel=null;
const mk=(cls,name,props={})=>({cls,name,props,kids:[],parent:null,open:true});
const add=(p,c)=>{c.parent=p;p.kids.push(c);return c};
const isPart=i=>['Part','SpawnLocation','WedgePart','MeshPart','TrussPart','CornerWedgePart'].includes(i.cls);
const part=(name,pos,size,col,shape='Block')=>mk('Part',name,{pos:[...pos],size:[...size],cf:null,col:[...col],tr:0,shape});

function newPlace(){
  root=mk('DataModel','Game');
  const ws=add(root,mk('Workspace','Workspace'));
  add(ws,part('Baseplate',[0,-0.5,0],[512,1,512],[.39,.65,.35]));
  add(ws,part('Part',[0,5,0],[4,4,4],[.64,.3,.28]));
  add(ws,part('Ball',[10,4,-6],[6,6,6],[.2,.45,.9],'Ball'));
  for(const c of ['Lighting','ReplicatedStorage','ServerScriptService'])add(root,mk(c,c));
  sel=null;
}

// ---- rbxlx reader / writer ----
function parseXML(txt){
  const d=new DOMParser().parseFromString(txt,'text/xml');
  if(d.querySelector('parsererror'))throw new Error('Invalid XML (binary .rbxl is not supported yet)');
  const num=(e,t)=>parseFloat(e.querySelector(':scope > '+t)?.textContent||0);
  const walk=(el,parent)=>{
    const it=mk(el.getAttribute('class'),el.getAttribute('class'));
    const P=el.querySelector(':scope > Properties');
    if(P)for(const p of P.children){
      const n=(p.getAttribute('name')||'').toLowerCase(),t=p.textContent;
      if(n==='name')it.name=t;
      else if(n==='size'&&p.tagName==='Vector3')it.props.size=[num(p,'X'),num(p,'Y'),num(p,'Z')];
      else if(n==='cframe'&&p.tagName==='CoordinateFrame'){
        const k=['X','Y','Z','R00','R01','R02','R10','R11','R12','R20','R21','R22'];
        it.props.cf=k.map(x=>num(p,x));it.props.pos=it.props.cf.slice(0,3);}
      else if(p.tagName==='Color3uint8'&&(n==='color3uint8'||n==='color')){const v=+t>>>0;it.props.col=[(v>>16&255)/255,(v>>8&255)/255,(v&255)/255];}
      else if(p.tagName==='Color3'&&n==='color')it.props.col=[num(p,'R'),num(p,'G'),num(p,'B')];
      else if(n==='transparency')it.props.tr=parseFloat(t)||0;
      else if(n==='shape')it.props.shape=({0:'Ball',1:'Block',2:'Cylinder'})[t]||t;
    }
    if(isPart(it)){const q=it.props;q.size??=[4,1,2];q.pos??=[0,0,0];q.col??=[.64,.64,.64];q.tr??=0;q.shape??='Block';}
    parent&&add(parent,it);
    for(const c of el.children)if(c.tagName==='Item')walk(c,it);
    return it;
  };
  const r=mk('DataModel','Game');
  for(const el of d.documentElement.children)if(el.tagName==='Item')add(r,walk(el,null)),r.kids.at(-1).parent=r;
  return r;
}
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;');
function writeXML(){
  let n=0;
  const w=i=>{
    let s=`<Item class="${i.cls}" referent="RBX${n++}"><Properties><string name="Name">${esc(i.name)}</string>`;
    if(isPart(i)){const p=i.props,c=p.cf?[...p.cf]:[...p.pos,1,0,0,0,1,0,0,0,1];c[0]=p.pos[0];c[1]=p.pos[1];c[2]=p.pos[2];
      const k=['X','Y','Z','R00','R01','R02','R10','R11','R12','R20','R21','R22'];
      s+=`<CoordinateFrame name="CFrame">${k.map((x,j)=>`<${x}>${c[j]}</${x}>`).join('')}</CoordinateFrame>`;
      s+=`<Vector3 name="size"><X>${p.size[0]}</X><Y>${p.size[1]}</Y><Z>${p.size[2]}</Z></Vector3>`;
      s+=`<Color3uint8 name="Color3uint8">${(255<<24|Math.round(p.col[0]*255)<<16|Math.round(p.col[1]*255)<<8|Math.round(p.col[2]*255))>>>0}</Color3uint8>`;
      s+=`<float name="Transparency">${p.tr}</float><token name="shape">${p.shape==='Ball'?0:p.shape==='Cylinder'?2:1}</token>`;}
    return s+'</Properties>'+i.kids.map(w).join('')+'</Item>';
  };
  return `<roblox version="4">${root.kids.map(w).join('')}</roblox>`;
}

// ---- 3D viewport ----
const vp=$('vp'),R=new THREE.WebGLRenderer({antialias:true});
R.shadowMap.enabled=true;vp.appendChild(R.domElement);
const scene=new THREE.Scene();scene.background=new THREE.Color(0x87b5e6);scene.fog=new THREE.Fog(0x87b5e6,200,900);
const cam=new THREE.PerspectiveCamera(60,1,.1,2000);cam.position.set(18,14,22);
const ctl=new OrbitControls(cam,R.domElement);ctl.target.set(0,3,0);
scene.add(new THREE.HemisphereLight(0xffffff,0x667788,1.1));
const sun=new THREE.DirectionalLight(0xffffff,1.6);sun.position.set(60,100,40);sun.castShadow=true;
Object.assign(sun.shadow.camera,{left:-80,right:80,top:80,bottom:-80,far:300});sun.shadow.mapSize.set(2048,2048);scene.add(sun);
const grid=new THREE.GridHelper(512,128,0x000000,0x000000);grid.material.opacity=.12;grid.material.transparent=true;scene.add(grid);
const world=new THREE.Group();scene.add(world);
const box=new THREE.Box3Helper(new THREE.Box3(),0x00a2ff);box.visible=false;scene.add(box);
const meshOf=new Map();
const geo={Block:new THREE.BoxGeometry(1,1,1),Ball:new THREE.SphereGeometry(.5,32,16),Cylinder:new THREE.CylinderGeometry(.5,.5,1,32)};

function rebuild(){
  world.clear();meshOf.clear();
  (function walk(i){
    if(isPart(i)){
      const p=i.props,m=new THREE.Mesh(geo[p.shape]||geo.Block,new THREE.MeshStandardMaterial({color:new THREE.Color(...p.col),roughness:.8,transparent:p.tr>0,opacity:1-p.tr}));
      if(p.shape==='Cylinder')m.rotation.z=Math.PI/2; // Roblox cylinders run along X
      const g=new THREE.Group();g.add(m);
      if(p.cf){g.matrixAutoUpdate=false;g.matrix.set(p.cf[3],p.cf[4],p.cf[5],p.pos[0],p.cf[6],p.cf[7],p.cf[8],p.pos[1],p.cf[9],p.cf[10],p.cf[11],p.pos[2],0,0,0,1);}
      else g.position.set(...p.pos);
      m.scale.set(...p.size);m.castShadow=m.receiveShadow=true;m.userData.inst=i;
      world.add(g);meshOf.set(i,m);
    }
    i.kids.forEach(walk);
  })(root);
  highlight();
}
function highlight(){
  const m=sel&&meshOf.get(sel);box.visible=!!m;
  if(m){m.parent.updateMatrixWorld(true);box.box.setFromObject(m);}
}
function resize(){R.setSize(vp.clientWidth,vp.clientHeight);cam.aspect=vp.clientWidth/vp.clientHeight;cam.updateProjectionMatrix()}
new ResizeObserver(resize).observe(vp);
R.setAnimationLoop(()=>{ctl.update();R.render(scene,cam)});

let down=null;const ray=new THREE.Raycaster();
R.domElement.addEventListener('pointerdown',e=>down=[e.clientX,e.clientY]);
R.domElement.addEventListener('pointerup',e=>{
  if(!down||Math.hypot(e.clientX-down[0],e.clientY-down[1])>4)return;
  const r=R.domElement.getBoundingClientRect();
  ray.setFromCamera(new THREE.Vector2((e.clientX-r.left)/r.width*2-1,-(e.clientY-r.top)/r.height*2+1),cam);
  const h=ray.intersectObjects([...meshOf.values()])[0];
  select(h?h.object.userData.inst:null);
});

// ---- Explorer / Properties ----
function select(i){sel=i;drawExp();drawProps();highlight()}
function drawExp(){
  const e=$('exp');e.innerHTML='';
  (function walk(i,d){
    const row=document.createElement('div');row.className='n'+(i===sel?' s':'');row.style.paddingLeft=d*14+4+'px';
    row.textContent=(i.kids.length?(i.open?'▾ ':'▸ '):'• ')+i.name+'  ';
    const s=document.createElement('small');s.style.opacity=.5;s.textContent=i.cls;row.append(s);
    row.onclick=ev=>{if(ev.offsetX<d*14+16&&i.kids.length)i.open=!i.open;select(i)};
    e.append(row);if(i.open)i.kids.forEach(k=>walk(k,d+1));
  })(root,0);
}
function drawProps(){
  const P=$('prop');P.innerHTML='';
  if(!sel||sel===root){P.innerHTML='<h4>Properties</h4><div class="r">Nothing selected</div>';return}
  P.innerHTML=`<h4>${sel.cls}</h4>`;
  const row=(l,v,set)=>{const r=document.createElement('div');r.className='r';r.innerHTML=`<label>${l}</label>`;
    const i=document.createElement('input');i.value=v;i.onchange=()=>{set(i.value);rebuild();drawExp()};r.append(i);P.append(r)};
  row('Name',sel.name,v=>sel.name=v);
  if(!isPart(sel))return;
  const p=sel.props,vec=(a,l)=>row(l,a.map(x=>+x.toFixed(3)).join(', '),v=>{const n=v.split(',').map(Number);if(n.length===3&&n.every(isFinite))n.forEach((x,k)=>a[k]=x)});
  vec(p.pos,'Position');vec(p.size,'Size');
  row('Color (hex)','#'+new THREE.Color(...p.col).getHexString(),v=>{const c=new THREE.Color(v);p.col=[c.r,c.g,c.b]});
  row('Transparency',p.tr,v=>p.tr=Math.min(1,Math.max(0,+v||0)));
  row('Shape',p.shape,v=>{if(geo[v])p.shape=v});
}
const refresh=()=>{rebuild();drawExp();drawProps()};

// ---- toolbar ----
const target=()=>sel&&sel!==root?sel:root.kids.find(k=>k.cls==='Workspace');
const spawn=shape=>{const c=ctl.target;const q=part(shape==='Ball'?'Ball':'Part',[Math.round(c.x),Math.round(c.y)+2,Math.round(c.z)],shape==='Ball'?[4,4,4]:[4,2,4],[.64,.64,.64],shape);
  const t=target();add(isPart(t)?t.parent:t,q);sel=q;log('Added '+q.name);refresh()};
$('add').onclick=()=>spawn('Block');$('sph').onclick=()=>spawn('Ball');
$('del').onclick=()=>{if(!sel||sel===root||sel.cls==='Workspace')return;sel.parent.kids.splice(sel.parent.kids.indexOf(sel),1);log('Deleted '+sel.name);sel=null;refresh()};
$('dup').onclick=()=>{if(!sel||!isPart(sel))return;const c=structuredClone({...sel,parent:null,kids:[]});c.props.pos[0]+=sel.props.size[0]+1;if(c.props.cf)c.props.cf[0]=c.props.pos[0];add(sel.parent,c);sel=c;refresh()};
$('reset').onclick=()=>{newPlace();refresh();log('New place')};
$('open').onclick=()=>$('file').click();
$('file').onchange=async e=>{const f=e.target.files[0];if(!f)return;
  try{root=parseXML(await f.text());sel=null;
    let n=0;(function c(i){isPart(i)&&n++;i.kids.forEach(c)})(root);log(`Loaded ${f.name}: ${n} parts`);refresh();
    const b=new THREE.Box3().setFromObject(world);if(!b.isEmpty()){const s=b.getSize(new THREE.Vector3()),m=Math.min(Math.max(s.x,s.y,s.z),150);ctl.target.copy(b.getCenter(new THREE.Vector3()));cam.position.copy(ctl.target).add(new THREE.Vector3(m*.8,m*.6,m*.8))}
  }catch(x){log('Open failed: '+x.message)}e.target.value=''};
$('save').onclick=()=>{const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([writeXML()],{type:'text/xml'}));a.download='place.rbxlx';a.click();log('Saved place.rbxlx')};
addEventListener('keydown',e=>{if(e.target.tagName==='INPUT')return;if(e.key==='Delete')$('del').click();if(e.ctrlKey&&e.key==='d'){e.preventDefault();$('dup').click()}});

log('StudioRblx Web ready. Click parts to select; drag to orbit, right-drag to pan, scroll to zoom.');
newPlace();refresh();resize();
