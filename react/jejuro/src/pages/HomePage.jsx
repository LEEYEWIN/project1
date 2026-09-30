import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { errorMessage } from '../api/client.js';
import { categoryLabel } from '../utils/format.js';
import island from '../assets/home-jeju-island.png';
import '../styles/home.css';

function DestinationCard({ poi, index }) {
  const [imageFailed, setImageFailed] = useState(false);
  return <Link className={`home-destination home-tone-${index % 4}`} to={`/pois/${poi.poiId}`}>
    <div className="home-destination-image">
      {poi.imageUrl && !imageFailed ? <img src={poi.imageUrl} alt={poi.name} loading="lazy" onError={() => setImageFailed(true)} />
        : <div className="home-image-fallback"><span aria-hidden="true">✳</span><span>{categoryLabel(poi.categoryCode)}</span></div>}
      <span className="home-card-arrow" aria-hidden="true">↗</span>
    </div>
    <h3>{poi.name}</h3>
    <p>#{categoryLabel(poi.categoryCode)}{poi.regionName ? `　#${poi.regionName}` : ''}</p>
  </Link>;
}

export default function HomePage() {
  const [pois, setPois] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refresh, setRefresh] = useState(0);
  const request = useRef(0);
  useEffect(() => {
    const id = ++request.current;
    const controller = new AbortController();
    setLoading(true); setError('');
    client.get('/pois/discoveries', { signal: controller.signal })
      .then(({ data }) => { if (id === request.current) setPois(data); })
      .catch(err => { if (!controller.signal.aborted && id === request.current) setError(errorMessage(err)); })
      .finally(() => { if (!controller.signal.aborted && id === request.current) setLoading(false); });
    return () => controller.abort();
  }, [refresh]);
  return <main className="home-page">
    <section className="home-hero" aria-labelledby="home-title">
      <div className="home-wordmark" aria-hidden="true">JEJURO</div>
      <div className="home-hero-copy"><h1 id="home-title">같은 제주,<br />나만의 여행.</h1>
        <Link className="home-start" to="/travels/new">내 여행 취향 찾기 <span aria-hidden="true">↗</span></Link>
      </div>
      <div className="home-island-wrap"><div className="home-island-halo" /><img src={island} className="home-island" alt="성산일출봉, 돌하르방, 감귤과 푸른 바다로 꾸민 제주 섬" fetchPriority="high" /></div>
      <p className="home-hero-description">취향과 여행 조건으로 찾는<br />나에게 맞는 제주.</p>
    </section>
    <section className="home-discover" aria-labelledby="home-discover-title">
      <div className="home-section-heading"><div><p className="home-kicker">DISCOVER JEJU</p><h2 id="home-discover-title">취향을 따라 만나는 제주</h2></div><Link to="/pois">모든 관광지 보기 <span aria-hidden="true">→</span></Link></div>
      <div className="home-discover-tools"><p>서로 다른 카테고리에서 만나는 네 가지 여행의 시작.</p><button type="button" onClick={() => setRefresh(v => v + 1)} disabled={loading}>다른 장소 보기 <span aria-hidden="true">↻</span></button></div>
      {error ? <div className="home-state" role="alert"><p>{error}</p><button onClick={() => setRefresh(v => v + 1)}>다시 불러오기</button></div>
        : loading ? <div className="home-destinations" aria-busy="true" aria-label="관광지를 불러오는 중">{[0,1,2,3].map(i => <div key={i} className={`home-card-skeleton home-tone-${i}`}><span /><span /></div>)}</div>
        : pois.length ? <div className="home-destinations">{pois.map((poi, i) => <DestinationCard key={poi.poiId} poi={poi} index={i} />)}</div>
        : <p className="home-state">아직 등록된 관광지가 없습니다.</p>}
    </section>
    <footer className="home-footer">JEJURO <span>나의 취향으로 완성하는 제주</span></footer>
  </main>;
}
