/** 원본/EXIF를 전송하지 않고 브라우저에서 JPEG로 정규화한다. */
export async function prepareFoodPhoto(file: File): Promise<Blob> {
  if (!["image/jpeg", "image/png"].includes(file.type)) throw new Error("JPG 또는 PNG 사진을 선택해주세요.");
  if (file.size > 5 * 1024 * 1024) throw new Error("사진은 5 MiB 이하로 선택해주세요.");
  const bitmap = await createImageBitmap(file);
  try {
    if (bitmap.width * bitmap.height > 16_000_000) throw new Error("이미지는 1600만 픽셀 이하여야 합니다.");
    const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const context = canvas.getContext("2d");
    if (!context) throw new Error("사진을 읽지 못했어요. 다른 사진을 선택해주세요.");
    context.fillStyle = "white";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    return await new Promise<Blob>((resolve, reject) => canvas.toBlob(
      (blob) => blob ? resolve(blob) : reject(new Error("사진을 변환하지 못했어요.")), "image/jpeg", 0.85,
    ));
  } finally { bitmap.close(); }
}
