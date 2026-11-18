<!doctype html>
<html lang="bn">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#0b0e14">
<title>YouTube Explore</title>
<style>
:root{
  color-scheme:dark;
  --bg:#0b0e14;
  --surface:#111722;
  --surface-raised:#171f2c;
  --line:rgba(255,255,255,.085);
  --text:#f4f6fb;
  --muted:#929caf;
  --accent:#ff4d6d;
  --accent-strong:#ed3154;
  --green:#41d6a0;
  --page-width:1440px;
}
*{box-sizing:border-box}
html{scroll-behavior:smooth}
body{
  min-width:320px;
  margin:0;
  background:
    radial-gradient(ellipse at 12% 0%,rgba(54,75,126,.19),transparent 34rem),
    radial-gradient(ellipse at 92% 18%,rgba(255,77,109,.075),transparent 30rem),
    var(--bg);
  color:var(--text);
  font-family:Inter,Arial,"Noto Sans Bengali",sans-serif;
  -webkit-font-smoothing:antialiased;
}
button,input,select{font:inherit}
button{color:inherit}
button:focus-visible,input:focus-visible,select:focus-visible,.card:focus-visible{
  outline:3px solid rgba(255,77,109,.65);
  outline-offset:3px;
}
.site-header{
  position:sticky;
  top:0;
  z-index:5;
  border-bottom:1px solid var(--line);
  background:rgba(11,14,20,.78);
  backdrop-filter:blur(18px);
}
.header-inner{
  width:min(var(--page-width),100%);
  min-height:78px;
  margin:auto;
  padding:12px 36px;
  display:flex;
  align-items:center;
  gap:44px;
}
.brand{display:flex;align-items:center;gap:11px;flex:0 0 auto;color:inherit;text-decoration:none}
.brand:focus-visible{outline:3px solid rgba(255,77,109,.65);outline-offset:4px;border-radius:8px}
.brand-mark{
  width:40px;height:40px;
  display:grid;place-items:center;
  border:1px solid rgba(255,255,255,.16);
  border-radius:14px;
  background:linear-gradient(145deg,#ff647d,#df284d);
  color:white;
  font-size:15px;
  box-shadow:0 8px 24px rgba(237,49,84,.2);
}
.brand-name{display:flex;gap:4px;font-size:18px;font-weight:800;letter-spacing:-.6px}
.brand-name span{color:#ff7189}
#searchForm{
  display:flex;
  width:min(600px,100%);
  height:46px;
  margin:0 auto;
  overflow:hidden;
  border:1px solid var(--line);
  border-radius:14px;
  background:rgba(255,255,255,.045);
  transition:border-color .2s,background .2s,box-shadow .2s;
}
#searchForm:focus-within{
  border-color:rgba(255,77,109,.55);
  background:rgba(255,255,255,.065);
  box-shadow:0 0 0 4px rgba(255,77,109,.08);
}
#q{
  width:100%;min-width:0;
  padding:0 16px;
  border:0;outline:0;
  background:transparent;
  color:var(--text);
  font-size:14px;
}
#q::placeholder{color:#7f899b}
#searchForm button{
  width:50px;flex:0 0 50px;
  display:grid;place-items:center;
  border:0;
  background:transparent;
  color:#b5bfd0;
  cursor:pointer;
  transition:color .2s,background .2s;
}
#searchForm button:hover{background:rgba(255,255,255,.07);color:white}
main{
  width:min(var(--page-width),100%);
  margin:auto;
  padding:50px 36px 72px;
}
.hero{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:28px;
  margin-bottom:30px;
}
.eyebrow{
  margin:0 0 12px;
  color:#ff8094;
  font-size:11px;
  font-weight:800;
  letter-spacing:2px;
  text-transform:uppercase;
}
h1{
  margin:0;
  font-size:clamp(28px,4vw,43px);
  font-weight:800;
  letter-spacing:-1.6px;
  line-height:1.15;
}
.sub{margin:11px 0 0;color:var(--muted);font-size:14px;line-height:1.7}
.tools{display:flex;flex:0 0 auto}
button{cursor:pointer}
.downloaderBtn,.downloaderForm button,.downloadControls button{
  min-height:46px;
  border:1px solid rgba(255,255,255,.1);
  border-radius:13px;
  background:linear-gradient(135deg,var(--accent),var(--accent-strong));
  box-shadow:0 8px 24px rgba(237,49,84,.16);
  color:white;
  font-size:14px;
  font-weight:700;
  transition:transform .2s,filter .2s,box-shadow .2s;
}
.downloaderBtn{padding:0 18px}
.downloaderBtn:hover,.downloaderForm button:hover,.downloadControls button:hover{
  transform:translateY(-1px);
  filter:brightness(1.08);
  box-shadow:0 10px 28px rgba(237,49,84,.24);
}
.downloaderWrap{
  display:none;
  margin:0 0 26px;
  padding:22px;
  border:1px solid var(--line);
  border-radius:20px;
  background:linear-gradient(145deg,rgba(23,31,44,.96),rgba(17,23,34,.96));
  box-shadow:0 18px 48px rgba(0,0,0,.18);
}
.downloaderWrap.open{display:block;animation:panel-in .24s ease-out}
.downloaderWrap h2{margin:0 0 16px;font-size:18px;letter-spacing:-.3px}
.downloaderForm{display:flex;gap:10px;max-width:760px;margin:0}
.downloaderForm input{
  flex:1;min-width:0;height:46px;
  padding:0 14px;
  border:1px solid var(--line);
  border-radius:12px;
  background:rgba(0,0,0,.2);
  color:var(--text);
  outline:0;
}
.downloaderForm input:focus{border-color:rgba(255,77,109,.55)}
.downloaderForm button{min-width:108px;padding:0 18px}
.downloaderResult{
  display:none;
  max-width:760px;
  margin-top:16px;
  padding:18px;
  border:1px solid var(--line);
  border-radius:15px;
  background:rgba(0,0,0,.16);
}
.downloaderMeta{margin-bottom:14px;line-height:1.6}
.downloadControls{display:flex;gap:10px}
.downloadControls select,.playerDownload select{
  min-width:0;
  border:1px solid var(--line);
  border-radius:11px;
  background:#101620;
  color:var(--text);
}
.downloadControls select{flex:1;padding:0 12px}
.downloadControls button{min-width:130px;padding:0 16px;background:linear-gradient(135deg,#35c993,#169b70)}
.downloadControls button:hover{box-shadow:0 10px 28px rgba(22,155,112,.2)}
#downloadStatus{min-height:20px;margin:12px 0 0;color:var(--muted);font-size:13px}
.featured{
  position:relative;
  display:grid;
  grid-template-columns:minmax(0,.92fr) minmax(0,1.08fr);
  min-height:320px;
  margin:0 0 38px;
  overflow:hidden;
  border:1px solid rgba(255,255,255,.1);
  border-radius:24px;
  background:
    radial-gradient(ellipse at 12% 10%,rgba(255,77,109,.18),transparent 25rem),
    linear-gradient(125deg,#182132,#111722 66%);
  box-shadow:0 24px 70px rgba(0,0,0,.24);
}
.featured[hidden]{display:none}
.featured-copy{
  position:relative;
  z-index:1;
  display:flex;
  flex-direction:column;
  align-items:flex-start;
  justify-content:center;
  padding:clamp(24px,4vw,48px);
}
.featured-kicker{
  display:inline-flex;
  align-items:center;
  gap:9px;
  margin-bottom:18px;
  color:#ff9aaa;
  font-size:11px;
  font-weight:800;
  letter-spacing:1.6px;
}
.featured-kicker::before{
  content:"";
  width:7px;height:7px;
  border-radius:50%;
  background:var(--accent);
  box-shadow:0 0 14px rgba(255,77,109,.9);
}
.featured-copy h2{
  max-width:560px;
  margin:0;
  display:-webkit-box;
  overflow:hidden;
  color:#fff;
  font-size:clamp(24px,3vw,38px);
  font-weight:800;
  letter-spacing:-1px;
  line-height:1.28;
  -webkit-box-orient:vertical;
  -webkit-line-clamp:3;
}
.featured-channel{margin:14px 0 22px;color:#aab4c5;font-size:13px}
.featuredPlayButton{
  min-height:44px;
  display:inline-flex;
  align-items:center;
  gap:10px;
  padding:0 17px;
  border:1px solid rgba(255,255,255,.14);
  border-radius:12px;
  background:linear-gradient(135deg,var(--accent),var(--accent-strong));
  box-shadow:0 8px 26px rgba(237,49,84,.2);
  color:#fff;
  font-size:13px;
  font-weight:750;
  transition:transform .2s,filter .2s;
}
.featuredPlayButton:hover{transform:translateY(-2px);filter:brightness(1.08)}
.featured-art{position:relative;min-height:320px;overflow:hidden}
.featured-art::after{
  content:"";
  position:absolute;
  inset:0;
  background:linear-gradient(90deg,#141b28 0%,rgba(20,27,40,.45) 20%,transparent 62%),linear-gradient(0deg,rgba(8,11,16,.22),transparent 45%);
  pointer-events:none;
}
.featured-art img{width:100%;height:100%;position:absolute;inset:0;object-fit:cover}
.featured-badge{
  position:absolute;
  z-index:1;
  right:18px;top:18px;
  padding:8px 11px;
  border:1px solid rgba(255,255,255,.2);
  border-radius:10px;
  background:rgba(9,12,18,.62);
  color:#fff;
  font-size:11px;
  font-weight:700;
  backdrop-filter:blur(8px);
}
.chips{
  display:flex;
  gap:9px;
  overflow-x:auto;
  margin:25px 0 18px;
  padding:2px 2px 8px;
  scrollbar-width:thin;
  scrollbar-color:#343b49 transparent;
}
.chips::-webkit-scrollbar{display:none}
.chip{
  flex:0 0 auto;
  min-height:38px;
  padding:0 15px;
  border:1px solid var(--line);
  border-radius:12px;
  background:rgba(255,255,255,.035);
  color:#b4bdcc;
  font-size:13px;
  font-weight:600;
  transition:background .2s,border-color .2s,color .2s,transform .2s;
}
.chip:hover{transform:translateY(-1px);border-color:rgba(255,255,255,.2);color:white}
.chip.active{
  border-color:rgba(255,77,109,.32);
  background:rgba(255,77,109,.13);
  color:#ff8094;
}
#status{min-height:20px;margin:14px 0 18px;color:var(--muted);font-size:13px}
.grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:30px 20px}
.card{
  min-width:0;
  cursor:pointer;
  border-radius:16px;
  transition:transform .22s;
}
.card:hover{transform:translateY(-3px)}
.thumb{
  position:relative;
  aspect-ratio:16/9;
  overflow:hidden;
  border:1px solid rgba(255,255,255,.07);
  border-radius:15px;
  background:#151b26;
  box-shadow:0 10px 28px rgba(0,0,0,.18);
}
.thumb::after{
  content:"▶";
  position:absolute;
  left:50%;top:50%;
  width:48px;height:48px;
  display:grid;place-items:center;
  padding-left:3px;
  border:1px solid rgba(255,255,255,.3);
  border-radius:50%;
  background:rgba(12,15,22,.72);
  color:white;
  font-size:16px;
  opacity:0;
  transform:translate(-50%,-44%) scale(.9);
  transition:opacity .2s,transform .2s;
  backdrop-filter:blur(8px);
}
.card:hover .thumb::after,.card:focus-visible .thumb::after{opacity:1;transform:translate(-50%,-50%) scale(1)}
.thumb img{width:100%;height:100%;display:block;object-fit:cover;transition:transform .45s}
.card:hover .thumb img{transform:scale(1.045)}
.info{display:flex;gap:12px;padding:13px 3px 0}
.dot{
  width:36px;height:36px;flex:0 0 36px;
  display:grid;place-items:center;
  border:1px solid rgba(255,255,255,.1);
  border-radius:50%;
  background:linear-gradient(145deg,#282f3e,#1b2230);
  color:#ff8296;
  font-size:13px;
}
.title{
  display:-webkit-box;
  overflow:hidden;
  color:#eef1f7;
  font-size:14px;
  font-weight:700;
  line-height:1.5;
  -webkit-box-orient:vertical;
  -webkit-line-clamp:2;
}
.meta{margin-top:5px;color:var(--muted);font-size:12px;line-height:1.55}
#playerWrap{
  display:none;
  margin:22px 0 34px;
  padding:20px;
  border:1px solid var(--line);
  border-radius:20px;
  background:linear-gradient(145deg,rgba(23,31,44,.96),rgba(17,23,34,.96));
  box-shadow:0 20px 55px rgba(0,0,0,.25);
}
#playerTitle{margin:0 0 14px;font-size:18px;font-weight:750;line-height:1.5}
.player{
  position:relative;
  width:100%;
  max-width:1050px;
  aspect-ratio:16/9;
  overflow:hidden;
  border:1px solid rgba(255,255,255,.08);
  border-radius:15px;
  background:#050608;
}
#youtubePlayerMount,#youtubePlayerMount iframe{position:absolute;inset:0;width:100%;height:100%;border:0}
.playerCenterPlay{
  position:absolute;
  z-index:2;
  top:50%;left:50%;
  width:66px;height:66px;
  display:grid;place-items:center;
  border:1px solid rgba(255,255,255,.35);
  border-radius:50%;
  background:rgba(255,77,109,.92);
  color:white;
  box-shadow:0 10px 38px rgba(0,0,0,.4);
  transform:translate(-50%,-50%);
  transition:transform .2s,filter .2s;
}
.playerCenterPlay:hover{filter:brightness(1.1);transform:translate(-50%,-50%) scale(1.06)}
.playerCenterPlay:disabled,.playerControlButton:disabled{opacity:.55;cursor:not-allowed}
.playerCenterPlay[hidden],.playerMessage[hidden]{display:none}
.playerCenterPlay svg{width:24px;height:24px}
.playerMessage{
  position:absolute;
  z-index:2;
  top:14px;left:14px;
  max-width:calc(100% - 28px);
  padding:8px 11px;
  border:1px solid rgba(255,255,255,.12);
  border-radius:9px;
  background:rgba(8,11,16,.82);
  color:#e7ebf3;
  font-size:12px;
}
#player:fullscreen{
  width:100vw;height:100vh;max-width:none;aspect-ratio:auto;border-radius:0;
}
.customPlayerControls{
  position:absolute;
  z-index:3;
  right:0;bottom:0;left:0;
  display:grid;
  grid-template-columns:38px auto minmax(60px,1fr) auto 38px minmax(54px,90px) 38px;
  align-items:center;
  gap:10px;
  padding:38px 14px 12px;
  background:linear-gradient(transparent,rgba(4,6,10,.94));
}
.playerControlButton{
  width:38px;height:38px;
  display:grid;place-items:center;
  border:0;border-radius:10px;
  background:transparent;
  color:#f4f6fb;
  transition:background .18s,color .18s;
}
.playerControlButton:hover{background:rgba(255,255,255,.14);color:white}
.playerControlButton svg{width:19px;height:19px}
.playerTime{
  color:#f0f2f7;
  font-size:11px;
  font-variant-numeric:tabular-nums;
  white-space:nowrap;
}
.playerSeek,.playerVolume{width:100%;margin:0;accent-color:var(--accent);cursor:pointer}
.playerVolume{max-width:90px}
.playerDownload{display:none;align-items:center;gap:10px;flex-wrap:wrap;margin-top:16px}
.playerDownload button{
  min-height:42px;
  padding:0 17px;
  border:1px solid rgba(255,255,255,.1);
  border-radius:11px;
  background:linear-gradient(135deg,var(--accent),var(--accent-strong));
  color:white;
  font-size:13px;
  font-weight:700;
  transition:filter .2s,transform .2s;
}
.playerDownload button[hidden],.downloadProgress[hidden]{display:none}
.playerDownload button:hover:not(:disabled){filter:brightness(1.08);transform:translateY(-1px)}
.playerDownload button:disabled,.playerDownload select:disabled{opacity:.58;cursor:not-allowed}
.playerDownload select{height:42px;max-width:220px;padding:0 12px}
.playerDownloadStatus{min-height:20px}
#playerDownloadStatus{min-height:20px;margin:10px 0 0;color:var(--muted);font-size:13px}
.downloadProgress{display:flex;align-items:center;gap:12px;margin-top:15px;color:var(--muted);font-size:13px}
.progressTrack{width:min(300px,60vw);height:5px;overflow:hidden;border-radius:5px;background:#303847}
.progressBar{width:35%;height:100%;border-radius:5px;background:linear-gradient(90deg,#ff8094,#ff4d6d);animation:download-progress 1.2s ease-in-out infinite}
@keyframes download-progress{from{transform:translateX(-110%)}to{transform:translateX(310%)}}
@keyframes panel-in{from{opacity:0;transform:translateY(-5px)}to{opacity:1;transform:translateY(0)}}
@media(max-width:1100px){.grid{grid-template-columns:repeat(3,minmax(0,1fr))}.header-inner{gap:28px}}
@media(max-width:800px){
  .header-inner{min-height:70px;padding:10px 22px;gap:18px}
  main{padding:38px 22px 56px}
  .grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:25px 16px}
  .featured{grid-template-columns:minmax(0,1fr) minmax(0,.95fr);min-height:280px}
  .featured-art{min-height:280px}
  .featured-copy{padding:25px}
}
@media(max-width:560px){
  .header-inner{min-height:66px;padding:9px 15px;gap:12px}
  .brand{gap:8px}
  .brand-mark{width:36px;height:36px;border-radius:12px}
  .brand-name{font-size:16px}
  #searchForm{height:42px}
  #q{padding:0 11px;font-size:13px}
  #searchForm button{width:42px;flex-basis:42px}
  main{padding:30px 15px 44px}
  .hero{align-items:flex-start;flex-direction:column;gap:18px;margin-bottom:23px}
  .featured{grid-template-columns:1fr;margin-bottom:30px;border-radius:19px}
  .featured-copy{padding:24px 20px 20px}
  .featured-kicker{margin-bottom:12px}
  .featured-copy h2{font-size:23px}
  .featured-channel{margin:10px 0 16px}
  .featured-art{min-height:0;aspect-ratio:16/9;grid-row:1}
  .featured-art::after{background:linear-gradient(0deg,rgba(8,11,16,.3),transparent 65%)}
  .featured-badge{right:12px;top:12px}
  h1{letter-spacing:-1px}
  .sub{font-size:13px}
  .downloaderBtn{min-height:42px}
  .downloaderWrap{padding:17px;border-radius:16px}
  .downloaderForm{flex-direction:column}
  .downloaderForm button{width:100%}
  .downloadControls{flex-direction:column}
  .downloadControls select,.downloadControls button{min-height:44px}
  .chips{margin-top:21px;margin-right:-15px}
  .grid{gap:21px 12px}
  .title{font-size:13px}
  .dot{display:none}
  #playerWrap{padding:13px;border-radius:16px}
  #playerTitle{font-size:16px}
  .player{border-radius:11px}
  .customPlayerControls{
    grid-template-columns:36px auto minmax(40px,1fr) auto 36px 36px;
    gap:6px;
    padding:32px 8px 8px;
  }
  .playerControlButton{width:36px;height:36px}
  .playerVolume{display:none}
}
@media(max-width:390px){
  .brand-name{display:none}
  .grid{grid-template-columns:1fr;gap:22px}
  .card{display:flex;gap:11px;align-items:flex-start}
  .thumb{width:44%;flex:0 0 44%;border-radius:12px}
  .info{min-width:0;padding:1px 0 0}
  .title{font-size:13px}
  .meta{font-size:11px}
}
@media(prefers-reduced-motion:reduce){
  *,*::before,*::after{scroll-behavior:auto!important;animation-duration:.01ms!important;animation-iteration-count:1!important;transition-duration:.01ms!important}
}
.site-header{height:64px;background:#0f0f0f}
.header-inner{width:100%;height:64px;min-height:0;padding:0 22px;display:flex;gap:28px}
#menuToggle,.iconButton{
  width:40px;height:40px;flex:0 0 40px;
  display:grid;place-items:center;
  border:0;border-radius:50%;
  background:transparent;color:#f1f1f1;
}
#menuToggle:hover,.iconButton:hover{background:#272727}
#menuToggle svg{width:24px;height:24px}
.brand{width:180px;gap:12px}
.brand-mark{width:34px;height:26px;border:0;border-radius:8px;background:#ff0033;font-size:12px;box-shadow:none}
.brand-name{font-size:18px;font-weight:700;letter-spacing:-.3px}
.brand-name span{color:#f1f1f1}
#searchForm{height:40px;flex:1;max-width:640px;margin:0 auto;border:1px solid #303030;border-radius:22px;background:#121212}
#searchForm:focus-within{border-color:#3e3e3e;box-shadow:inset 1px 0 #1c62b9}
#q{padding-left:18px;font-size:16px}
#searchForm button{width:64px;flex-basis:64px;border-left:1px solid #303030;background:#222;border-radius:0 22px 22px 0}
.header-actions{width:180px;display:flex;justify-content:flex-end}
#toggleDownloader{min-height:38px;border-radius:20px;padding:0 15px;background:#272727;box-shadow:none;font-size:13px}
#toggleDownloader:hover{background:#383838;transform:none;box-shadow:none}
.app-layout{min-height:calc(100vh - 64px);display:grid;grid-template-columns:220px minmax(0,1fr)}
.sidebar{
  position:sticky;top:64px;
  height:calc(100vh - 64px);
  overflow-y:auto;
  padding:12px 12px 24px;
  background:var(--bg);
  scrollbar-width:thin;
  scrollbar-color:#3c3c3c transparent;
}
.sidebar-section{padding:0 0 12px;margin:0 0 12px;border-bottom:1px solid #303030}
.sidebar-section-label{padding:9px 12px 6px;color:#aaa;font-size:12px;font-weight:700}
.sidebarLink{
  width:100%;min-height:42px;
  display:flex;align-items:center;gap:22px;
  padding:0 12px;
  border:0;border-radius:10px;
  background:transparent;color:#f1f1f1;
  text-align:left;font-size:14px;
  white-space:nowrap;overflow:hidden;
}
.sidebarLink svg{width:22px;height:22px;flex:0 0 22px}
.sidebarLink:hover{background:#272727}
.sidebarLink.active{background:#272727;font-weight:700}
.sidebarDivider{height:1px;margin:12px 0;background:#303030}
#sidebarOverlay{display:none}
@media(min-width:761px){
  .app-layout.sidebar-collapsed{grid-template-columns:72px minmax(0,1fr)}
  .app-layout.sidebar-collapsed .sidebar{padding:12px 6px}
  .app-layout.sidebar-collapsed .sidebar-section-label,.app-layout.sidebar-collapsed .nav-label{display:none}
  .app-layout.sidebar-collapsed .sidebarLink{min-height:64px;flex-direction:column;justify-content:center;gap:5px;padding:5px 2px;font-size:10px}
}
.main-shell{width:100%;max-width:none;min-width:0;margin:0;padding:0 24px 48px}
.downloaderWrap{margin:16px 0}
.chips{
  position:sticky;top:64px;z-index:3;
  gap:12px;margin:0 -24px 18px;padding:12px 24px;
  border-bottom:1px solid rgba(255,255,255,.07);
  background:rgba(15,15,15,.96);
  scrollbar-width:none;
}
.chip{min-height:34px;padding:0 13px;border:0;border-radius:8px;background:#272727;color:#f1f1f1;font-size:13px}
.chip:hover{background:#3f3f3f;transform:none}
.chip.active{background:#f1f1f1;color:#0f0f0f}
.feedHeading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:20px 0 18px}
.feedHeading h1{margin:0;font-size:20px;font-weight:600;letter-spacing:0;line-height:1.35}
#status{min-height:20px;margin:0;color:#aaa;text-align:right}
.grid{grid-template-columns:repeat(4,minmax(0,1fr));gap:38px 18px}
.card{border-radius:12px}
.card:hover{transform:none}
.thumb{border:0;border-radius:12px;box-shadow:none}
.info{gap:12px;padding:12px 0 0}
.dot{width:36px;height:36px;flex-basis:36px;border:0;background:#303030;color:#f1f1f1;font-size:15px}
.title{font-size:14px;line-height:1.45}
.meta{margin-top:5px;font-size:12px;color:#aaa}
#playerWrap{margin:16px 0 28px;padding:18px;border-radius:16px;background:#181818;box-shadow:none}
@media(max-width:1300px){.grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:1050px){
  .app-layout{grid-template-columns:72px minmax(0,1fr)}
  .sidebar{padding:12px 6px}
  .sidebar-section-label,.nav-label{display:none}
  .sidebarLink{min-height:64px;flex-direction:column;justify-content:center;gap:5px;padding:5px 2px;font-size:10px}
  .sidebarLink svg{width:22px;height:22px}
}
@media(min-width:761px) and (max-width:1050px){
  .app-layout.sidebar-expanded{grid-template-columns:220px minmax(0,1fr)}
  .app-layout.sidebar-expanded .sidebar{padding:12px}
  .app-layout.sidebar-expanded .sidebar-section-label,.app-layout.sidebar-expanded .nav-label{display:block}
  .app-layout.sidebar-expanded .sidebarLink{min-height:42px;flex-direction:row;justify-content:flex-start;gap:22px;padding:0 12px;font-size:14px}
}
@media(max-width:760px){
  .header-inner{gap:12px;padding:0 14px}
  .brand{width:auto;gap:9px}
  .brand-name{font-size:16px}
  .header-actions{width:auto}
  #toggleDownloader{width:40px;padding:0;font-size:0}
  #toggleDownloader::before{content:"⬇";font-size:17px}
  .app-layout{display:block}
  .sidebar{
    position:fixed;z-index:6;top:64px;bottom:0;left:0;
    width:250px;height:auto;
    padding:12px;
    border-right:1px solid #303030;
    background:#0f0f0f;
    transform:translateX(-105%);
    transition:transform .2s ease;
  }
  .app-layout.mobile-open .sidebar{transform:translateX(0)}
  .app-layout.mobile-open #sidebarOverlay{
    position:fixed;z-index:5;inset:64px 0 0;
    display:block;border:0;background:rgba(0,0,0,.62)
  }
  .app-layout .sidebar-section-label,.app-layout .nav-label{display:block}
  .app-layout .sidebarLink{min-height:44px;flex-direction:row;justify-content:flex-start;gap:18px;padding:0 12px;font-size:14px}
  .main-shell{padding:0 16px 40px}
  .chips{top:64px;margin:0 -16px 16px;padding:11px 16px;gap:10px}
  .grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:28px 16px}
  .feedHeading{margin-top:16px}
}
@media(max-width:520px){
  .header-inner{gap:8px;padding:0 10px}
  .brand{gap:0}
  .brand-name{display:none}
  #searchForm{height:38px}
  #q{padding-left:12px;font-size:14px}
  #searchForm button{width:48px;flex-basis:48px}
  .main-shell{padding-right:12px;padding-left:12px}
  .chips{margin-right:-12px;margin-left:-12px;padding-right:12px;padding-left:12px}
  .grid{grid-template-columns:1fr;gap:25px}
  .thumb{border-radius:12px}
  .info{padding-top:10px}
  .title{font-size:15px}
  .meta{font-size:12px}
}
</style>
</head>
<body>
<header class="site-header">
  <div class="header-inner">
  <button id="menuToggle" type="button" aria-label="Toggle navigation" aria-expanded="true">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
      <path d="M4 6h16M4 12h16M4 18h16"/>
    </svg>
  </button>
  <a class="brand" href="./" aria-label="YouTube Explore home">
    <span class="brand-mark" aria-hidden="true">▶</span>
    <span class="brand-name">YT<span>Explore</span></span>
  </a>
  <form id="searchForm" role="search">
    <input id="q" type="search" placeholder="গান, ভিডিও বা চ্যানেল সার্চ করুন..." aria-label="Search YouTube">
    <button type="submit" aria-label="Search">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="10.8" cy="10.8" r="6.8" stroke="currentColor" stroke-width="1.8"/>
        <path d="m16 16 4.2 4.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
      </svg>
    </button>
  </form>
  <div class="header-actions">
    <button class="downloaderBtn" id="toggleDownloader" type="button">⬇ YouTube Downloader</button>
  </div>
  </div>
</header>
<div class="app-layout" id="appLayout">
  <aside class="sidebar" id="sidebar" aria-label="Main navigation">
    <div class="sidebar-section">
      <button class="sidebarLink active" id="popularBtn" type="button">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z"/></svg>
        <span class="nav-label">Home</span>
      </button>
      <button class="sidebarLink" type="button" data-q="Bangladesh music">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
        <span class="nav-label">Music</span>
      </button>
      <button class="sidebarLink" type="button" data-q="gaming">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 9h12a4 4 0 0 1 3.8 5.2l-1 3.1a2.5 2.5 0 0 1-4.1 1.1L14 17h-4l-2.7 1.4a2.5 2.5 0 0 1-4.1-1.1l-1-3.1A4 4 0 0 1 6 9Z"/><path d="M7 12v4m-2-2h4m7-1h.01M18 15h.01"/></svg>
        <span class="nav-label">Gaming</span>
      </button>
    </div>
    <div class="sidebar-section">
      <div class="sidebar-section-label">Explore</div>
      <button class="sidebarLink" type="button" data-q="বাংলা গান">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="m19 15 .9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9L19 15Z"/></svg>
        <span class="nav-label">বাংলা গান</span>
      </button>
      <button class="sidebarLink" type="button" data-q="comedy Bangladesh">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01"/></svg>
        <span class="nav-label">Comedy</span>
      </button>
      <button class="sidebarLink" type="button" data-q="news Bangladesh">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 8h4v4H7zm7 0h3m-3 4h3m-10 4h10"/></svg>
        <span class="nav-label">News</span>
      </button>
    </div>
  </aside>
  <button id="sidebarOverlay" type="button" aria-label="Close navigation"></button>
  <main class="main-shell">
  <section class="downloaderWrap" id="downloaderWrap">
    <h2>YouTube Downloader</h2>
    <form class="downloaderForm" id="downloaderForm">
      <input id="videoUrl" type="url" placeholder="YouTube video URL দিন..." aria-label="YouTube video URL">
      <button id="fetchInfoBtn" type="submit">Search</button>
    </form>
    <div class="downloaderResult" id="downloadResult">
      <div class="downloaderMeta" id="videoMeta"></div>
      <div class="downloadControls">
        <select id="resolution" aria-label="Download resolution">
          <option value="highest">Highest Quality</option>
        </select>
        <button id="downloadBtn" type="button">Download</button>
      </div>
    </div>
    <div id="downloadStatus" role="status"></div>
  </section>
  <nav class="chips" aria-label="Video categories">
    <button class="chip" type="button" data-q="বাংলা গান">বাংলা গান</button>
    <button class="chip" type="button" data-q="Bangladesh music">Music</button>
    <button class="chip" type="button" data-q="comedy Bangladesh">Comedy</button>
    <button class="chip" type="button" data-q="gaming">Gaming</button>
    <button class="chip" type="button" data-q="news Bangladesh">News</button>
  </nav>
  <div class="feedHeading">
    <h1 id="heading">Home</h1>
    <div id="status" role="status">ভিডিও লোড হচ্ছে...</div>
  </div>
  <section id="playerWrap">
    <div id="playerTitle"></div>
    <div class="player" id="player">
      <div id="youtubePlayerMount"></div>
      <button class="playerCenterPlay" id="playerCenterPlay" type="button" aria-label="Play video" hidden></button>
      <div class="playerMessage" id="playerMessage" role="status" hidden></div>
      <div class="customPlayerControls" aria-label="Video controls">
        <button class="playerControlButton" id="playerPlayPause" type="button" aria-label="Play"></button>
        <span class="playerTime" id="playerCurrentTime">0:00</span>
        <input class="playerSeek" id="playerSeek" type="range" min="0" max="0" step="0.1" value="0" aria-label="Seek video">
        <span class="playerTime" id="playerDuration">0:00</span>
        <button class="playerControlButton" id="playerMute" type="button" aria-label="Mute"></button>
        <input class="playerVolume" id="playerVolume" type="range" min="0" max="100" step="1" value="100" aria-label="Volume">
        <button class="playerControlButton" id="playerFullscreen" type="button" aria-label="Enter fullscreen"></button>
      </div>
    </div>
    <div class="playerDownload" id="playerDownload">
      <select id="playerResolution" aria-label="Download resolution" disabled>
        <option value="">Resolution লোড হচ্ছে...</option>
      </select>
      <button id="generatePlayerDownload" type="button" disabled>Generate</button>
      <button id="downloadGeneratedVideo" type="button" hidden>Download</button>
    </div>
    <div class="downloadProgress" id="playerDownloadProgress" role="progressbar" aria-label="ডাউনলোড লিংক তৈরি হচ্ছে" hidden>
      <div class="progressTrack"><div class="progressBar"></div></div>
      <span>ডাউনলোড লিংক তৈরি হচ্ছে...</span>
    </div>
    <div id="playerDownloadStatus" role="status"></div>
  </section>
  <section class="grid" id="results" aria-label="Video results"></section>
  </main>
</div>
<script>
const results = document.getElementById('results');
const statusEl = document.getElementById('status');
const heading = document.getElementById('heading');
const playerWrap = document.getElementById('playerWrap');
const player = document.getElementById('player');
const playerTitle = document.getElementById('playerTitle');
const youtubePlayerMount = document.getElementById('youtubePlayerMount');
const playerCenterPlay = document.getElementById('playerCenterPlay');
const playerMessage = document.getElementById('playerMessage');
const playerPlayPause = document.getElementById('playerPlayPause');
const playerCurrentTime = document.getElementById('playerCurrentTime');
const playerDuration = document.getElementById('playerDuration');
const playerSeek = document.getElementById('playerSeek');
const playerMute = document.getElementById('playerMute');
const playerVolume = document.getElementById('playerVolume');
const playerFullscreen = document.getElementById('playerFullscreen');
const playerDownload = document.getElementById('playerDownload');
const playerResolution = document.getElementById('playerResolution');
const generatePlayerDownload = document.getElementById('generatePlayerDownload');
const downloadGeneratedVideo = document.getElementById('downloadGeneratedVideo');
const playerDownloadProgress = document.getElementById('playerDownloadProgress');
const playerDownloadStatus = document.getElementById('playerDownloadStatus');
const qInput = document.getElementById('q');
const popularBtn = document.getElementById('popularBtn');
const appLayout = document.getElementById('appLayout');
const menuToggle = document.getElementById('menuToggle');
const sidebarOverlay = document.getElementById('sidebarOverlay');
let currentVideoUrl = '';
let currentVideoId = '';
let currentSearchQuery = '';
let playerDownloadUrl = '';
let playerRequestId = 0;
let youtubePlayer = null;
let youtubePlayerReady = false;
let youtubeApiFailed = false;
let playerProgressTimer = null;
let isSeeking = false;

const PLAY_ICON = '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M7 4.8v14.4a.8.8 0 0 0 1.2.7l11-7.2a.8.8 0 0 0 0-1.4l-11-7.2a.8.8 0 0 0-1.2.7Z"/></svg>';
const PAUSE_ICON = '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M6.5 5.5A1.5 1.5 0 0 1 8 4h1a1.5 1.5 0 0 1 1.5 1.5v13A1.5 1.5 0 0 1 9 20H8a1.5 1.5 0 0 1-1.5-1.5v-13Zm7 0A1.5 1.5 0 0 1 15 4h1a1.5 1.5 0 0 1 1.5 1.5v13A1.5 1.5 0 0 1 16 20h-1a1.5 1.5 0 0 1-1.5-1.5v-13Z"/></svg>';
const VOLUME_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M11 5 6 9H3v6h3l5 4V5Z"/><path d="M15.5 8.5a5 5 0 0 1 0 7"/><path d="M18.5 5.5a9 9 0 0 1 0 13"/></svg>';
const MUTED_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M11 5 6 9H3v6h3l5 4V5Z"/><path d="m16 9 5 6m0-6-5 6"/></svg>';
const FULLSCREEN_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M8 3H5a2 2 0 0 0-2 2v3m13-5h3a2 2 0 0 1 2 2v3M3 16v3a2 2 0 0 0 2 2h3m13-5v3a2 2 0 0 1-2 2h-3"/></svg>';

function safeDate(value) {
  if (!value) return '';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('bn-BD');
}
function setPlaybackUi(playing) {
  playerPlayPause.innerHTML = playing ? PAUSE_ICON : PLAY_ICON;
  playerPlayPause.setAttribute('aria-label', playing ? 'Pause' : 'Play');
  playerCenterPlay.innerHTML = PLAY_ICON;
  playerCenterPlay.hidden = playing;
}
function formatTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';
  const total = Math.floor(seconds);
  const minutes = Math.floor(total / 60);
  const remaining = String(total % 60).padStart(2, '0');
  if (minutes < 60) return minutes + ':' + remaining;
  return Math.floor(minutes / 60) + ':' + String(minutes % 60).padStart(2, '0') + ':' + remaining;
}
function updatePlayerProgress() {
  if (!youtubePlayerReady) return;
  const duration = youtubePlayer.getDuration() || 0;
  const currentTime = youtubePlayer.getCurrentTime() || 0;
  playerSeek.max = String(duration);
  if (!isSeeking) {
    playerSeek.value = String(currentTime);
    playerCurrentTime.textContent = formatTime(currentTime);
  }
  playerDuration.textContent = formatTime(duration);
}
function createYoutubePlayer() {
  if (!window.YT || !window.YT.Player || youtubePlayer || !currentVideoId) return;
  const initialVideoId = currentVideoId;
  youtubePlayer = new YT.Player('youtubePlayerMount', {
    host: 'https://www.youtube-nocookie.com',
    width: '100%',
    height: '100%',
    videoId: initialVideoId,
    playerVars: {
      autoplay: 1,
      controls: 0,
      disablekb: 1,
      fs: 0,
      playsinline: 1,
      rel: 0,
      iv_load_policy: 3,
      origin: window.location.origin
    },
    events: {
      onReady: event => {
        youtubePlayerReady = true;
        const iframe = event.target.getIframe();
        iframe.title = playerTitle.textContent || 'YouTube video player';
        playerPlayPause.disabled = false;
        playerCenterPlay.disabled = false;
        if (currentVideoId !== initialVideoId) {
          event.target.loadVideoById(currentVideoId);
        } else {
          event.target.playVideo();
        }
        updateMuteControl();
        playerMessage.hidden = true;
        if (playerProgressTimer === null) {
          playerProgressTimer = window.setInterval(updatePlayerProgress, 250);
        }
      },
      onStateChange: event => {
        const isActive = event.data === YT.PlayerState.PLAYING ||
          event.data === YT.PlayerState.BUFFERING;
        setPlaybackUi(isActive);
        if (event.data !== YT.PlayerState.UNSTARTED) playerMessage.hidden = true;
        updateMuteControl();
        updatePlayerProgress();
      },
      onError: event => {
        setPlaybackUi(false);
        playerMessage.textContent = 'এই ভিডিওটি চালানো যাচ্ছে না (YouTube error ' + event.data + ')।';
        playerMessage.hidden = false;
      }
    }
  });
}
function toggleYoutubePlayback() {
  if (!youtubePlayerReady) return;
  const state = youtubePlayer.getPlayerState();
  if (state === YT.PlayerState.PLAYING || state === YT.PlayerState.BUFFERING) {
    youtubePlayer.pauseVideo();
  } else {
    youtubePlayer.playVideo();
  }
}
function updateMuteControl() {
  const muted = youtubePlayerReady &&
    (youtubePlayer.isMuted() || youtubePlayer.getVolume() === 0);
  playerMute.innerHTML = muted ? MUTED_ICON : VOLUME_ICON;
  playerMute.setAttribute('aria-label', muted ? 'Unmute' : 'Mute');
}
playerPlayPause.innerHTML = PLAY_ICON;
playerCenterPlay.innerHTML = PLAY_ICON;
playerMute.innerHTML = VOLUME_ICON;
playerFullscreen.innerHTML = FULLSCREEN_ICON;
playerPlayPause.addEventListener('click', toggleYoutubePlayback);
playerCenterPlay.addEventListener('click', toggleYoutubePlayback);
playerSeek.addEventListener('pointerdown', () => { isSeeking = true; });
playerSeek.addEventListener('input', () => {
  isSeeking = true;
  playerCurrentTime.textContent = formatTime(Number(playerSeek.value));
});
playerSeek.addEventListener('change', () => {
  if (youtubePlayerReady) youtubePlayer.seekTo(Number(playerSeek.value), true);
  isSeeking = false;
  updatePlayerProgress();
});
playerMute.addEventListener('click', () => {
  if (!youtubePlayerReady) return;
  if (youtubePlayer.isMuted()) youtubePlayer.unMute();
  else youtubePlayer.mute();
  updateMuteControl();
});
playerVolume.addEventListener('input', () => {
  if (!youtubePlayerReady) return;
  const volume = Number(playerVolume.value);
  youtubePlayer.setVolume(volume);
  if (volume > 0 && youtubePlayer.isMuted()) youtubePlayer.unMute();
  updateMuteControl();
});
playerFullscreen.addEventListener('click', async () => {
  try {
    if (document.fullscreenElement === player) {
      await document.exitFullscreen();
    } else if (player.requestFullscreen) {
      await player.requestFullscreen();
    } else {
      throw new Error('Fullscreen এই browser-এ সমর্থিত নয়।');
    }
  } catch (error) {
    playerMessage.textContent = error.message;
    playerMessage.hidden = false;
  }
});
document.addEventListener('fullscreenchange', () => {
  playerFullscreen.setAttribute(
    'aria-label',
    document.fullscreenElement === player ? 'Exit fullscreen' : 'Enter fullscreen'
  );
});
function playVideo(video, updateHistory = true) {
  if (!/^[A-Za-z0-9_-]{11}$/.test(video.id)) return;
  currentVideoUrl = 'https://www.youtube.com/watch?v=' + encodeURIComponent(video.id);
  currentVideoId = video.id;
  const requestId = ++playerRequestId;
  playerDownloadUrl = '';
  playerTitle.textContent = video.title;
  playerWrap.style.display = 'block';
  playerMessage.textContent = 'ভিডিও লোড হচ্ছে...';
  playerMessage.hidden = false;
  playerPlayPause.disabled = !youtubePlayerReady;
  playerCenterPlay.disabled = !youtubePlayerReady;
  playerSeek.value = '0';
  playerCurrentTime.textContent = '0:00';
  playerDuration.textContent = '0:00';
  setPlaybackUi(false);
  playerDownload.style.display = 'flex';
  playerResolution.disabled = true;
  playerResolution.replaceChildren(new Option('Resolution লোড হচ্ছে...', ''));
  generatePlayerDownload.disabled = true;
  generatePlayerDownload.hidden = false;
  downloadGeneratedVideo.hidden = true;
  playerDownloadProgress.hidden = true;
  playerDownloadStatus.style.color = '#bbb';
  playerDownloadStatus.textContent = '';
  if (updateHistory) updateBrowserUrl();
  if (youtubeApiFailed) {
    playerMessage.textContent = 'YouTube player লোড করা যায়নি। পেজ রিফ্রেশ করে চেষ্টা করুন।';
  } else if (youtubePlayerReady) {
    youtubePlayer.loadVideoById(currentVideoId);
    youtubePlayer.playVideo();
  } else if (window.YT && window.YT.Player) {
    createYoutubePlayer();
  }
  loadPlayerResolutions(currentVideoUrl, requestId);
  playerWrap.scrollIntoView({behavior:'smooth', block:'start'});
}
function render(items) {
  results.replaceChildren();
  if (!items.length) {
    statusEl.textContent = 'কোনো ভিডিও পাওয়া যায়নি। অন্য শব্দ দিয়ে চেষ্টা করুন।';
    return;
  }
  statusEl.textContent = items.length + 'টি ভিডিও';
  for (const video of items) {
    const card = document.createElement('article');
    card.className = 'card';
    card.tabIndex = 0;
    card.setAttribute('role', 'button');
    card.setAttribute('aria-label', 'Play ' + video.title);
    const thumb = document.createElement('div');
    thumb.className = 'thumb';
    const img = document.createElement('img');
    img.src = video.thumbnail;
    img.alt = '';
    img.loading = 'lazy';
    thumb.appendChild(img);

    const info = document.createElement('div');
    info.className = 'info';
    const dot = document.createElement('div');
    dot.className = 'dot';
    dot.textContent = video.channel ? video.channel.trim().charAt(0).toUpperCase() : '▶';
    const textBox = document.createElement('div');
    textBox.style.minWidth = '0';
    const title = document.createElement('div');
    title.className = 'title';
    title.textContent = video.title;
    const channel = document.createElement('div');
    channel.className = 'meta';
    channel.textContent = video.channel || '';
    const meta = document.createElement('div');
    meta.className = 'meta';
    const views = Number(video.views);
    meta.textContent = [
      Number.isFinite(views) && views > 0 ? views.toLocaleString('bn-BD') + ' views' : '',
      safeDate(video.publishedAt)
    ].filter(Boolean).join(' · ');
    textBox.append(title, channel, meta);
    info.append(dot, textBox);
    card.append(thumb, info);
    card.addEventListener('click', () => playVideo(video));
    card.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault(); playVideo(video);
      }
    });
    results.appendChild(card);
  }
}
async function loadVideos(action='popular', query='', selectedVideoId='') {
  statusEl.textContent = 'লোড হচ্ছে...';
  results.replaceChildren();
  try {
    const url = 'api.php?action=' + encodeURIComponent(action) +
      (query ? '&q=' + encodeURIComponent(query) : '');
    const response = await fetch(url);
    const data = await response.json();
    if (!response.ok || data.error) throw new Error(data.error || 'Request failed');
    const items = data.items || [];
    render(items);
    if (selectedVideoId) {
      const selectedVideo = items.find(video => video.id === selectedVideoId) ||
        {id:selectedVideoId, title:'YouTube video'};
      playVideo(selectedVideo, false);
    }
  } catch (err) {
    statusEl.textContent = 'সমস্যা হয়েছে: ' + err.message +
      ' — API key, API restriction ও PHP cURL সেটিংস পরীক্ষা করুন।';
    if (selectedVideoId) {
      playVideo({id:selectedVideoId, title:'YouTube video'}, false);
    }
  }
}
function updateBrowserUrl() {
  const url = new URL(window.location.href);
  const videoId = playerWrap.style.display === 'block' ? currentVideoId : '';
  if (currentSearchQuery) url.searchParams.set('q', currentSearchQuery);
  else url.searchParams.delete('q');
  if (videoId) url.searchParams.set('v', videoId);
  else url.searchParams.delete('v');
  const nextLocation = url.pathname + url.search + url.hash;
  const currentLocation = window.location.pathname + window.location.search + window.location.hash;
  if (nextLocation !== currentLocation) {
    window.history.pushState({query:currentSearchQuery, videoId}, '', nextLocation);
  }
}
function setActiveNavigation(query = '') {
  document.querySelectorAll('.sidebarLink').forEach(button => {
    button.classList.toggle(
      'active',
      button.id === 'popularBtn' ? !query : button.dataset.q === query
    );
  });
  document.querySelectorAll('.chip').forEach(button => {
    button.classList.toggle('active', button.dataset.q === query);
  });
}
function updateMenuState() {
  const isMobile = window.matchMedia('(max-width: 760px)').matches;
  const isCompactDesktop = window.matchMedia('(min-width: 761px) and (max-width: 1050px)').matches;
  const expanded = isMobile
    ? appLayout.classList.contains('mobile-open')
    : isCompactDesktop
      ? appLayout.classList.contains('sidebar-expanded')
      : !appLayout.classList.contains('sidebar-collapsed');
  menuToggle.setAttribute('aria-expanded', String(expanded));
}
function closeMobileNavigation() {
  if (window.matchMedia('(max-width: 760px)').matches) {
    appLayout.classList.remove('mobile-open');
    updateMenuState();
  }
}
function restoreBrowserUrl() {
  const url = new URL(window.location.href);
  currentSearchQuery = url.searchParams.get('q') || '';
  const requestedVideoId = url.searchParams.get('v') || '';
  const videoId = /^[A-Za-z0-9_-]{11}$/.test(requestedVideoId) ? requestedVideoId : '';
  qInput.value = currentSearchQuery;
  heading.textContent = currentSearchQuery
    ? 'সার্চ ফলাফল: ' + currentSearchQuery
    : 'Home';
  setActiveNavigation(currentSearchQuery);
  closeMobileNavigation();
  if (!videoId) {
    if (youtubePlayerReady) youtubePlayer.pauseVideo();
    playerWrap.style.display = 'none';
    currentVideoId = '';
    currentVideoUrl = '';
  }
  loadVideos(currentSearchQuery ? 'search' : 'popular', currentSearchQuery, videoId);
}
document.getElementById('searchForm').addEventListener('submit', e => {
  e.preventDefault();
  const q = qInput.value.trim();
  if (!q) return;
  heading.textContent = 'সার্চ ফলাফল: ' + q;
  currentSearchQuery = q;
  setActiveNavigation(q);
  closeMobileNavigation();
  updateBrowserUrl();
  loadVideos('search', q);
});
popularBtn.addEventListener('click', () => {
  qInput.value = '';
  heading.textContent = 'Home';
  currentSearchQuery = '';
  setActiveNavigation('');
  closeMobileNavigation();
  updateBrowserUrl();
  loadVideos('popular');
});
document.querySelectorAll('[data-q]').forEach(btn => {
  btn.addEventListener('click', () => {
    qInput.value = btn.dataset.q;
    heading.textContent = btn.querySelector('.nav-label')?.textContent || btn.textContent.trim();
    currentSearchQuery = btn.dataset.q;
    setActiveNavigation(currentSearchQuery);
    closeMobileNavigation();
    updateBrowserUrl();
    loadVideos('search', btn.dataset.q);
  });
});
window.addEventListener('popstate', restoreBrowserUrl);
menuToggle.addEventListener('click', () => {
  if (window.matchMedia('(max-width: 760px)').matches) {
    appLayout.classList.remove('sidebar-collapsed', 'sidebar-expanded');
    appLayout.classList.toggle('mobile-open');
  } else if (window.matchMedia('(max-width: 1050px)').matches) {
    appLayout.classList.remove('sidebar-collapsed');
    appLayout.classList.toggle('sidebar-expanded');
  } else {
    appLayout.classList.remove('sidebar-expanded');
    appLayout.classList.toggle('sidebar-collapsed');
  }
  updateMenuState();
});
sidebarOverlay.addEventListener('click', closeMobileNavigation);
window.addEventListener('resize', () => {
  if (window.matchMedia('(max-width: 760px)').matches) {
    appLayout.classList.remove('sidebar-collapsed', 'sidebar-expanded');
  }
  updateMenuState();
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') closeMobileNavigation();
});
updateMenuState();

const API_BASE = 'https://ytdlp-production-4d018ba3.up.railway.app';
const downloaderWrap = document.getElementById('downloaderWrap');
const downloaderForm = document.getElementById('downloaderForm');
const videoUrl = document.getElementById('videoUrl');
const fetchInfoBtn = document.getElementById('fetchInfoBtn');
const downloadResult = document.getElementById('downloadResult');
const videoMeta = document.getElementById('videoMeta');
const resolution = document.getElementById('resolution');
const downloadBtn = document.getElementById('downloadBtn');
const downloadStatus = document.getElementById('downloadStatus');

document.getElementById('toggleDownloader').addEventListener('click', () => {
  downloaderWrap.classList.toggle('open');
  if (downloaderWrap.classList.contains('open')) videoUrl.focus();
});

function setDownloadStatus(message, color = '#bbb') {
  downloadStatus.textContent = message;
  downloadStatus.style.color = color;
}

async function getJson(url, options) {
  const response = await fetch(url, options);
  let data;
  try {
    data = await response.json();
  } catch {
    data = null;
  }
  if (!response.ok) throw new Error(data?.error || data?.message || 'API request failed');
  return data;
}

function postDownloadApi(path, url) {
  return getJson(API_BASE + path, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({url})
  });
}

let playerDownloadFilename = '';

function safeFileName(title, quality) {
  const base = String(title || 'video').replace(/[\\/:*?"<>|]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 80) || 'video';
  const label = /^\d+$/.test(String(quality)) ? quality + 'p' : String(quality);
  return base + '-' + label + '.mp4';
}

// "Highest Quality" -> the biggest real resolution in the list.
function pickQuality(selectEl) {
  if (selectEl.value !== 'highest') return selectEl.value;
  const heights = Array.from(selectEl.options)
    .map(option => Number(option.value))
    .filter(number => Number.isFinite(number) && number > 0);
  return heights.length ? String(Math.max(...heights)) : '1080';
}

// Works with both backends:
//  - new backend: JSON with download_url
//  - older backend: the video file itself in the response (saved via a blob link)
async function requestVideoDownload(quality, url, title) {
  const response = await fetch(API_BASE + '/download/' + encodeURIComponent(quality), {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({url})
  });
  const type = (response.headers.get('Content-Type') || '').toLowerCase();

  if (type.includes('json')) {
    let data = null;
    try { data = await response.json(); } catch { data = null; }
    if (!response.ok || !data) throw new Error(data?.error || data?.message || 'API request failed');
    if (!data.download_url) throw new Error(data.error || 'Download link not found.');
    return {...data, download_url: resolveDownloadUrl(data.download_url), isBlob: false};
  }

  if (!response.ok) throw new Error('API request failed (' + response.status + ')');
  const blob = await response.blob();
  if (!blob.size) throw new Error('Server returned an empty file.');
  return {
    download_url: URL.createObjectURL(blob),
    isBlob: true,
    filename: safeFileName(title, quality)
  };
}

function saveBlobLink(href, filename) {
  const link = document.createElement('a');
  link.href = href;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

function resolveDownloadUrl(value) {
  // Works for both absolute links and relative ones like /file/<token>.
  return new URL(String(value), API_BASE).href;
}

function qualityLabel(value) {
  const text = String(value);
  return /^\d+$/.test(text) ? text + 'p' : text;
}

function getResolutions(data) {
  return Array.isArray(data)
    ? data
    : (Array.isArray(data?.resolutions)
      ? data.resolutions
      : (Array.isArray(data?.all)
        ? data.all
        : (Array.isArray(data?.progressive) ? data.progressive : [])));
}

function validateDownloadResolution(data, requestedQuality) {
  const match = String(requestedQuality).match(/^(\d+)p?$/i);
  const selectedHeight = Number(data?.selected?.height);
  if (match && Number.isFinite(selectedHeight) && selectedHeight !== Number(match[1])) {
    throw new Error(
      `API ${requestedQuality} না দিয়ে ${selectedHeight}p link দিয়েছে। Download API-তে quality selection ঠিক করতে হবে।`
    );
  }
}

async function loadPlayerResolutions(url, requestId) {
  playerDownloadStatus.textContent = 'Resolution লোড হচ্ছে...';
  try {
    const data = await postDownloadApi('/available_resolutions', url);
    if (requestId !== playerRequestId) return;
    const values = getResolutions(data);
    if (!values.length) throw new Error('কোনো resolution পাওয়া যায়নি।');
    playerResolution.replaceChildren(new Option('Resolution বেছে নিন', ''));
    for (const value of values) {
      const quality = String(value);
      if (!quality || quality === 'highest') continue;
      playerResolution.add(new Option(qualityLabel(quality), quality));
    }
    if (playerResolution.options.length < 2) throw new Error('কোনো resolution পাওয়া যায়নি।');
    playerResolution.disabled = false;
    playerDownloadStatus.textContent = values.length === 1
      ? 'এই ভিডিওর জন্য API শুধু ' + String(values[0]) + ' resolution দিয়েছে।'
      : 'ডাউনলোড করতে একটি resolution বেছে নিন।';
  } catch (error) {
    if (requestId !== playerRequestId) return;
    playerDownloadStatus.textContent = 'Error: ' + error.message;
    playerDownloadStatus.style.color = '#ff6b6b';
  } finally {
    if (requestId === playerRequestId && playerResolution.disabled) {
        playerResolution.replaceChildren(new Option('Resolution পাওয়া যায়নি', ''));
    }
  }
}

playerResolution.addEventListener('change', () => {
  const quality = playerResolution.value;
  playerDownloadUrl = '';
  downloadGeneratedVideo.hidden = true;
  generatePlayerDownload.disabled = !quality;
  playerDownloadStatus.style.color = '#bbb';
  playerDownloadStatus.textContent = quality ? 'Generate চাপুন।' : 'ডাউনলোড করতে একটি resolution বেছে নিন।';
});

generatePlayerDownload.addEventListener('click', async () => {
  const quality = playerResolution.value;
  if (!quality || !currentVideoUrl || generatePlayerDownload.disabled) return;
  const requestId = playerRequestId;
  const requestUrl = currentVideoUrl;
  generatePlayerDownload.disabled = true;
  playerResolution.disabled = true;
  playerDownloadProgress.hidden = false;
  playerDownloadStatus.style.color = '#bbb';
  playerDownloadStatus.textContent = '';
  downloadGeneratedVideo.hidden = true;
  try {
    const data = await requestVideoDownload(quality, requestUrl, playerTitle.textContent);
    if (requestId !== playerRequestId) return;
    validateDownloadResolution(data, quality);
    if (!data.download_url) throw new Error(data.error || 'ডাউনলোড লিংক পাওয়া যায়নি।');
    playerDownloadUrl = data.download_url;
    playerDownloadFilename = data.isBlob ? data.filename : '';
    playerDownloadProgress.hidden = true;
    playerResolution.disabled = false;
    downloadGeneratedVideo.hidden = false;
    playerDownloadStatus.textContent = 'লিংক তৈরি হয়েছে। ডাউনলোড শুরু করতে Download চাপুন।';
  } catch (error) {
    if (requestId !== playerRequestId) return;
    playerDownloadProgress.hidden = true;
    playerDownloadStatus.textContent = 'Error: ' + error.message;
    playerDownloadStatus.style.color = '#ff6b6b';
    playerResolution.disabled = false;
    generatePlayerDownload.disabled = false;
  }
});

downloadGeneratedVideo.addEventListener('click', () => {
  if (!playerDownloadUrl) return;
  if (playerDownloadFilename) {
    saveBlobLink(playerDownloadUrl, playerDownloadFilename);
    return;
  }
  const link = document.createElement('a');
  link.href = playerDownloadUrl;
  link.target = '_blank';
  link.rel = 'noopener';
  link.click();
});

downloaderForm.addEventListener('submit', async event => {
  event.preventDefault();
  const url = videoUrl.value.trim();
  if (!url) {
    setDownloadStatus('দয়া করে একটি YouTube লিংক দিন।', '#ff6b6b');
    videoUrl.focus();
    return;
  }

  fetchInfoBtn.disabled = true;
  fetchInfoBtn.textContent = 'Loading...';
  downloadResult.style.display = 'none';
  setDownloadStatus('ভিডিওর তথ্য নেওয়া হচ্ছে...');

  try {
    let resolutionsError = '';
    const [info, resolutions] = await Promise.all([
      postDownloadApi('/video_info', url),
      postDownloadApi('/available_resolutions', url).catch(error => {
        resolutionsError = error.message;
        return null;
      })
    ]);

    videoMeta.replaceChildren();
    const title = document.createElement('strong');
    title.textContent = info.title || 'YouTube Video';
    const details = document.createElement('div');
    details.style.marginTop = '4px';
    details.style.color = '#bbb';
    const author = info.author || info.uploader || 'Unknown';
    const length = info.length ?? info.duration ?? 0;
    const views = Number(info.views ?? info.view_count ?? 0);
    details.textContent = `Author: ${author} · Length: ${length}s · Views: ${views.toLocaleString('en-BD')}`;
    videoMeta.append(title, details);

    resolution.replaceChildren();
    const highest = document.createElement('option');
    highest.value = 'highest';
    highest.textContent = 'Highest Quality';
    resolution.appendChild(highest);

    const values = getResolutions(resolutions);
    for (const value of values) {
      const text = String(value);
      if (!text || text === 'highest') continue;
      const option = document.createElement('option');
      option.value = text;
      option.textContent = qualityLabel(text);
      resolution.appendChild(option);
    }

    downloadResult.style.display = 'block';
    if (!values.length && resolutionsError) {
      setDownloadStatus('Resolution list not available: ' + resolutionsError, '#ff6b6b');
      return;
    }
    setDownloadStatus(values.length === 1
      ? 'এই ভিডিওর জন্য API শুধু ' + String(values[0]) + ' resolution দিয়েছে।'
      : '');
  } catch (error) {
    setDownloadStatus('Error: ' + error.message, '#ff6b6b');
  } finally {
    fetchInfoBtn.disabled = false;
    fetchInfoBtn.textContent = 'Search';
  }
});

downloadBtn.addEventListener('click', async () => {
  const url = videoUrl.value.trim();
  const quality = pickQuality(resolution);
  if (!url) return;

  downloadBtn.disabled = true;
  downloadBtn.textContent = 'Processing...';
  setDownloadStatus('ডাউনলোড প্রসেস শুরু হয়েছে, অপেক্ষা করুন...', '#60a5fa');
  const downloadTab = window.open('about:blank', '_blank');

  try {
    const data = await requestVideoDownload(quality, url, videoMeta.querySelector('strong')?.textContent);
    validateDownloadResolution(data, quality);
    if (!data.download_url) throw new Error(data.error || 'ডাউনলোড লিংক পাওয়া যায়নি।');
    if (data.isBlob) {
      if (downloadTab) downloadTab.close();
      saveBlobLink(data.download_url, data.filename);
    } else if (downloadTab) {
      downloadTab.location.href = data.download_url;
    } else {
      window.open(data.download_url, '_blank', 'noopener');
    }
    setDownloadStatus('সফলভাবে ডাউনলোড সম্পন্ন হয়েছে!', '#4ade80');
  } catch (error) {
    if (downloadTab) downloadTab.close();
    setDownloadStatus(error.message.includes('টাইমআউট')
      ? 'নেটওয়ার্ক timeout বা সার্ভার timeout হয়েছে।'
      : 'Error: ' + error.message, '#ff6b6b');
  } finally {
    downloadBtn.disabled = false;
    downloadBtn.textContent = 'Download';
  }
});

window.onYouTubeIframeAPIReady = () => {
  if (currentVideoId) createYoutubePlayer();
};
const youtubeApiScript = document.createElement('script');
youtubeApiScript.src = 'https://www.youtube.com/iframe_api';
youtubeApiScript.async = true;
youtubeApiScript.onerror = () => {
  youtubeApiFailed = true;
  if (!currentVideoId) return;
  playerMessage.textContent = 'YouTube player লোড করা যায়নি। পেজ রিফ্রেশ করে চেষ্টা করুন।';
  playerMessage.hidden = false;
};
document.head.appendChild(youtubeApiScript);

restoreBrowserUrl();
</script>
</body>
</html>