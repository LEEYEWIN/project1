import { useState } from 'react';
import { fetchDirections } from '../../api/directionsApi.js';
import { formatDate, formatDistance } from '../../utils/format.js';

/**
 * 일정 영수증 이미지(PNG) 저장
 * - 여행 이름·기간·동반자 + 일차별 방문지 + 구간 거리·예상 택시비 + 합계를 영수증 모양으로 그린다
 * - 라이브러리 없이 canvas로 직접 그려서 저장 (한글은 브라우저 기본 글꼴)
 * travel: TravelDetailResponse, route: RouteDetailResponse (확정 경로 또는 지금 경로)
 * 택시비는 저장 시점에 일차별 자동차 동선(/directions)을 불러와 계산, 실패한 날은 거리·요금 없이 그린다.
 */
const W = 720;
const PAD = 48;
const FONT = "'Pretendard', 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif";
const INK = '#1B2430';
const SUB = '#596270';

const won = (n) => `${Number(n).toLocaleString()}원`;

export default function ReceiptButton({ travel, route }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const save = async () => {
    setBusy(true);
    setError('');
    try {
      // 일차별 자동차 동선 (택시비·거리). 한 날이 실패해도 나머지는 그린다
      const dirs = await Promise.all(
        route.days.map((d) => (d.spots.length > 1 ? fetchDirections(route.routeId, d.dayNo, 'CAR').catch(() => null) : null)),
      );
      const canvas = drawReceipt(travel, route, dirs);
      canvas.toBlob((blob) => {
        if (!blob) {
          setError('이미지를 만들지 못했어요.');
          return;
        }
        const href = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = href;
        link.download = `제주로_일정영수증_${travel.travelName}.png`;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(href);
      }, 'image/png');
    } catch {
      setError('영수증을 만들지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="receipt-actions">
      <button type="button" className="btn ghost small" disabled={busy} onClick={save}>
        {busy ? '영수증 만드는 중…' : '🧾 일정 영수증 이미지로 저장'}
      </button>
      {error && <span className="error-text">{error}</span>}
    </div>
  );
}

/** 영수증 그리기 → canvas */
function drawReceipt(travel, route, dirs) {
  // 1) 그릴 줄 목록 만들기 (높이 계산용)
  const rows = [];
  let totalSpots = 0;
  let totalDistance = 0;
  let totalFare = 0;
  route.days.forEach((d, i) => {
    const legs = dirs[i]?.legs ?? [];
    rows.push({ kind: 'day', text: `${d.dayNo}일차`, right: formatDate(d.date) });
    d.spots.forEach((s, j) => {
      totalSpots += 1;
      rows.push({ kind: 'spot', order: s.visitOrder, text: s.poi.name });
      const leg = legs[j];
      if (leg && j < d.spots.length - 1) {
        totalDistance += leg.distanceM;
        totalFare += leg.taxiFare ?? 0;
        rows.push({ kind: 'leg', text: `↓ ${formatDistance(leg.distanceM)}`, right: leg.taxiFare ? `택시 약 ${won(leg.taxiFare)}` : '' });
      }
    });
    if (d.spots.length === 0) rows.push({ kind: 'leg', text: '(방문지 없음)' });
    rows.push({ kind: 'gap' });
  });

  const lineH = { day: 54, spot: 39, leg: 28, gap: 12 };
  const bodyH = rows.reduce((h, r) => h + lineH[r.kind], 0);
  const H = 620 + bodyH;

  // 2) 캔버스 (선명하게 2배)
  const scale = 2;
  const canvas = document.createElement('canvas');
  canvas.width = W * scale;
  canvas.height = H * scale;
  const ctx = canvas.getContext('2d');
  ctx.scale(scale, scale);

  // 배경 + 영수증 종이 (아래 톱니)
  ctx.fillStyle = '#EDEAE3';
  ctx.fillRect(0, 0, W, H);
  ctx.fillStyle = '#FFFFFF';
  ctx.beginPath();
  ctx.moveTo(24, 24);
  ctx.lineTo(W - 24, 24);
  ctx.lineTo(W - 24, H - 40);
  for (let x = W - 24; x > 24; x -= 16) {
    ctx.lineTo(x - 8, H - 28);
    ctx.lineTo(x - 16, H - 40);
  }
  ctx.closePath();
  ctx.fill();

  let y = 80;
  const text = (s, x, yy, { size = 16, weight = 400, color = INK, align = 'left' } = {}) => {
    ctx.font = `${weight} ${size}px ${FONT}`;
    ctx.fillStyle = color;
    ctx.textAlign = align;
    ctx.fillText(s, x, yy);
  };
  const fit = (s, maxW, size, weight = 400) => {
    ctx.font = `${weight} ${size}px ${FONT}`;
    if (ctx.measureText(s).width <= maxW) return s;
    let t = s;
    while (t.length > 1 && ctx.measureText(`${t}…`).width > maxW) t = t.slice(0, -1);
    return `${t}…`;
  };
  const dashed = (yy) => {
    ctx.strokeStyle = '#B8B2A6';
    ctx.setLineDash([6, 5]);
    ctx.beginPath();
    ctx.moveTo(PAD, yy);
    ctx.lineTo(W - PAD, yy);
    ctx.stroke();
    ctx.setLineDash([]);
  };

  // 머리글
  text('JEJURO', W / 2, y, { size: 30, weight: 800, align: 'center' });
  y += 30;
  text('제주 여행 일정 영수증', W / 2, y, { size: 17, color: SUB, align: 'center' });
  y += 34;
  dashed(y);
  y += 42;
  text(fit(travel.travelName, W - PAD * 2, 24, 700), PAD, y, { size: 24, weight: 700 });
  y += 34;
  text(`${formatDate(travel.startDate)} ~ ${formatDate(travel.endDate)} · ${travel.tripDays}일`, PAD, y, { size: 17, color: SUB });
  y += 26;
  const who = travel.companions.length > 0 ? `동반 ${travel.companions.length}명` : '혼자';
  text(fit(`${who} · ${travel.regionNames.join(', ')}`, W - PAD * 2, 16), PAD, y, { size: 16, color: SUB });
  y += 29;
  dashed(y);
  y += 16;

  // 본문
  rows.forEach((r) => {
    if (r.kind === 'day') {
      y += 43;
      ctx.fillStyle = '#F5F3EF';
      ctx.fillRect(PAD, y - 31, W - PAD * 2, 45);
      text(r.text, PAD + 15, y, { size: 18, weight: 700 });
      text(r.right, W - PAD - 15, y, { size: 16, color: SUB, align: 'right' });
      y += 11;
    } else if (r.kind === 'spot') {
      y += 39;
      text(`${r.order}.`, PAD + 8, y, { size: 16, weight: 700, color: SUB });
      text(fit(r.text, W - PAD * 2 - 45, 17, 600), PAD + 40, y, { size: 17, weight: 600 });
    } else if (r.kind === 'leg') {
      y += 28;
      text(r.text, PAD + 40, y, { size: 15, color: SUB });
      if (r.right) text(r.right, W - PAD - 6, y, { size: 15, color: SUB, align: 'right' });
    } else {
      y += 12;
    }
  });

  // 합계
  y += 18;
  dashed(y);
  const sum = [
    ['방문지', `${totalSpots}곳`],
    ['자동차 이동 거리', totalDistance ? formatDistance(totalDistance) : '-'],
  ];
  sum.forEach(([k, v]) => {
    y += 34;
    text(k, PAD, y, { size: 16, color: SUB });
    text(v, W - PAD, y, { size: 17, weight: 600, align: 'right' });
  });
  y += 43;
  text('예상 택시비 합계', PAD, y, { size: 19, weight: 700 });
  text(totalFare ? won(totalFare) : '-', W - PAD, y, { size: 25, weight: 800, align: 'right' });
  y += 25;
  text('제주 중형택시 거리요금 기준 · 구간마다 따로 탄다고 가정', PAD, y, { size: 13, color: SUB });
  y += 19;
  text('심야할증 제외', PAD, y, { size: 13, color: SUB });

  // 바코드 모양 + 발행일
  y += 30;
  let seed = Number(travel.travelId) * 9301 + 49297;
  let x = PAD;
  ctx.fillStyle = INK;
  while (x < W - PAD) {
    seed = (seed * 9301 + 49297) % 233280;
    const w = 1 + (seed % 3);
    ctx.fillRect(x, y, w, 44);
    x += w + 1 + (seed % 4);
  }
  y += 66;
  const now = new Date();
  text(`발행 ${now.getFullYear()}.${now.getMonth() + 1}.${now.getDate()} · 여행 #${travel.travelId} · jejuro`, W / 2, y, {
    size: 14,
    color: SUB,
    align: 'center',
  });
  return canvas;
}
