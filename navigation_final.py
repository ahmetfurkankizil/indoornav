import open3d as o3d
import numpy as np
import matplotlib.pyplot as plt
from heapq import heappush, heappop

# --- AYARLAR ---
PLY_PATH = "reconstruction/cloud_and_poses.ply"
TARGET_RESOLUTION = 800  # Haritayı kaç piksel genişliğinde işleyelim? (Ne kadar yüksekse o kadar detaylı)
ROBOT_RADIUS = 3  # Duvarlardan kaç birim uzaktan gitsin?

def load_data(ply_path):
    print("1. Nokta bulutu okunuyor...")
    pcd = o3d.io.read_point_cloud(ply_path)
    points = np.asarray(pcd.points)
    
    if len(points) == 0: return None, None, None, None

    # Senin beğendiğin Görünüm A (X ve Z eksenleri)
    x = points[:, 0]
    z = points[:, 2]
    
    # Gürültü temizliği (Uzak noktaları at)
    points_2d = np.column_stack((x, z))
    mean = np.mean(points_2d, axis=0)
    std = np.std(points_2d, axis=0)
    points_clean = points_2d[np.all(np.abs(points_2d - mean) < 2.5 * std, axis=1)]
    
    return points_clean[:, 0], points_clean[:, 1]

def create_grid(x_points, z_points):
    """
    Nokta bulutunu A* için yüksek çözünürlüklü bir ızgaraya çevirir
    AMA bunu ekrana basmayacağız, sadece hesaplama için kullanacağız.
    """
    min_x, max_x = np.min(x_points), np.max(x_points)
    min_z, max_z = np.min(z_points), np.max(z_points)
    
    width_range = max_x - min_x
    height_range = max_z - min_z
    
    # Ölçek ne olursa olsun, en az 800 piksel genişliğinde detaylı bir harita oluştur
    grid_size = width_range / TARGET_RESOLUTION
    
    w = int(np.ceil(width_range / grid_size)) + 10
    h = int(np.ceil(height_range / grid_size)) + 10
    
    print(f"2. Arka plan hesaplama matrisi: {w}x{h} (Hassasiyet: {grid_size:.5f})")
    
    grid = np.zeros((h, w), dtype=int)
    
    # Noktaları gride işle
    # Koordinatları index'e çevir
    idx_x = ((x_points - min_x) / grid_size).astype(int) + 5
    idx_z = ((z_points - min_z) / grid_size).astype(int) + 5
    
    # Sınır kontrolleri
    valid = (idx_x >= 0) & (idx_x < w) & (idx_z >= 0) & (idx_z < h)
    grid[idx_z[valid], idx_x[valid]] = 1
    
    return grid, grid_size, min_x, min_z

def heuristic(a, b):
    return np.sqrt((b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2)

def a_star(matrix, start, goal):
    print("3. Rota hesaplanıyor...")
    neighbors = [(0, 1), (0, -1), (1, 0), (-1, 0), (1, 1), (1, -1), (-1, 1), (-1, -1)]
    close_set = set()
    came_from = {}
    g_score = {start: 0}
    f_score = {start: heuristic(start, goal)}
    oheap = []

    heappush(oheap, (f_score[start], start))

    while oheap:
        current = heappop(oheap)[1]
        if current == goal:
            data = []
            while current in came_from:
                data.append(current)
                current = came_from[current]
            return data[::-1]

        close_set.add(current)
        for i, j in neighbors:
            neighbor = current[0] + i, current[1] + j
            
            if 0 <= neighbor[0] < matrix.shape[0] and 0 <= neighbor[1] < matrix.shape[1]:
                if matrix[neighbor[0]][neighbor[1]] == 1: continue
            else: continue
                
            if neighbor in close_set and tentative_g_score >= g_score.get(neighbor, 0): continue
                
            tentative_g_score = g_score[current] + heuristic(current, neighbor)
            if tentative_g_score < g_score.get(neighbor, 0) or neighbor not in [i[1] for i in oheap]:
                came_from[neighbor] = current
                g_score[neighbor] = tentative_g_score
                f_score[neighbor] = tentative_g_score + heuristic(neighbor, goal)
                heappush(oheap, (f_score[neighbor], neighbor))
    return None

if __name__ == "__main__":
    # Veriyi Oku
    x_raw, z_raw = load_data(PLY_PATH)
    
    if x_raw is not None:
        # Hesaplama Gridini Oluştur
        grid, grid_size, min_x, min_z = create_grid(x_raw, z_raw)
        
        # --- GÖRSELLEŞTİRME (RAW SCATTER) ---
        fig, ax = plt.subplots(figsize=(10, 10))
        ax.set_title("Nokta Bulutu Navigasyonu (Tıklayarak Rota Çiz)")
        
        # İŞTE BU SATIR: Noktaları ızgara olarak değil, olduğu gibi basıyoruz.
        # s=1 (nokta boyutu), c='black' (renk), alpha=0.5 (hafif şeffaf)
        ax.scatter(x_raw, z_raw, s=1, c='black', alpha=0.6, label="Duvarlar")
        ax.axis('equal') # Oranları bozma
        
        print("Lütfen harita üzerinde BOŞ BİR YERE 2 kere tıkla (Başlangıç -> Bitiş)")
        
        try:
            coords = plt.ginput(2, timeout=60)
            
            if len(coords) == 2:
                # Tıklanan (float) koordinatları al
                start_real = coords[0]
                end_real = coords[1]
                
                # Bu koordinatları arka plandaki Grid indeksine çevir
                start_idx = (int((start_real[1] - min_z) / grid_size) + 5, int((start_real[0] - min_x) / grid_size) + 5)
                end_idx = (int((end_real[1] - min_z) / grid_size) + 5, int((end_real[0] - min_x) / grid_size) + 5)
                
                # A* Çalıştır
                path_idxs = a_star(grid, start_idx, end_idx)
                
                if path_idxs:
                    # Bulunan yol indekslerini tekrar Gerçek Koordinatlara çevir
                    path_x = [(p[1] - 5) * grid_size + min_x for p in path_idxs]
                    path_z = [(p[0] - 5) * grid_size + min_z for p in path_idxs]
                    
                    # Yolu Çiz
                    ax.plot(path_x, path_z, 'r-', linewidth=3, label="Rota")
                    ax.plot(start_real[0], start_real[1], 'go', markersize=10, label="Başlangıç")
                    ax.plot(end_real[0], end_real[1], 'bo', markersize=10, label="Bitiş")
                    ax.legend()
                    plt.show()
                else:
                    print("Yol Bulunamadı! (Duvarların içinden geçemem)")
            else:
                print("Eksik tıklama.")
        except Exception as e:
            print(f"Hata: {e}")