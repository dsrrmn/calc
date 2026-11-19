import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import java.util.Arrays;

/**
 * Generates custom procedural 3D meshes (TriangleMesh) for the calculator housing,
 * recessed bezels, circular metallic buttons, 4-way D-pad rockers, and sculpted keycaps.
 */
public final class ProceduralMeshFactory {

    public static final double BODY_W = 156.0;
    public static final double BODY_H = 320.0;
    public static final double BODY_D = 22.0;

    public static final double LCD_CENTER_X = 0.0;
    public static final double LCD_CENTER_Y = -82.0;
    public static final double LCD_OUTER_W = 126.0;
    public static final double LCD_OUTER_H = 66.0;

    public static final double SOLAR_CENTER_X = 36.5;
    public static final double SOLAR_CENTER_Y = -136.5;
    public static final double SOLAR_OUTER_W = 51.0;
    public static final double SOLAR_OUTER_H = 17.5;

    private ProceduralMeshFactory() {}

    private static final class MeshBuilder {
        private float[] points = new float[1024];
        private float[] normals = new float[1024];
        private float[] texCoords = new float[1024];
        private int[] faces = new int[2048];
        private int vCount = 0;
        private int fCount = 0;

        int addVertex(double x, double y, double z, double nx, double ny, double nz, double u, double v) {
            if (vCount * 3 + 3 > points.length) {
                points = Arrays.copyOf(points, points.length * 2);
                normals = Arrays.copyOf(normals, normals.length * 2);
            }
            if (vCount * 2 + 2 > texCoords.length) {
                texCoords = Arrays.copyOf(texCoords, texCoords.length * 2);
            }
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-9) {
                nx = 0; ny = 0; nz = -1;
            } else {
                nx /= len; ny /= len; nz /= len;
            }
            int idx = vCount++;
            points[idx * 3] = (float) x;
            points[idx * 3 + 1] = (float) y;
            points[idx * 3 + 2] = (float) z;
            normals[idx * 3] = (float) nx;
            normals[idx * 3 + 1] = (float) ny;
            normals[idx * 3 + 2] = (float) nz;
            texCoords[idx * 2] = (float) u;
            texCoords[idx * 2 + 1] = (float) v;
            return idx;
        }

        void addTriangle(int a, int b, int c) {
            if (a == b || b == c || a == c) return;
            if (fCount + 9 > faces.length) {
                faces = Arrays.copyOf(faces, faces.length * 2);
            }
            double ax = points[a * 3], ay = points[a * 3 + 1], az = points[a * 3 + 2];
            double bx = points[b * 3], by = points[b * 3 + 1], bz = points[b * 3 + 2];
            double cx = points[c * 3], cy = points[c * 3 + 1], cz = points[c * 3 + 2];

            double ux = bx - ax, uy = by - ay, uz = bz - az;
            double vx = cx - ax, vy = cy - ay, vz = cz - az;
            double fnx = uy * vz - uz * vy;
            double fny = uz * vx - ux * vz;
            double fnz = ux * vy - uy * vx;

            double vnx = normals[a * 3] + normals[b * 3] + normals[c * 3];
            double vny = normals[a * 3 + 1] + normals[b * 3 + 1] + normals[c * 3 + 1];
            double vnz = normals[a * 3 + 2] + normals[b * 3 + 2] + normals[c * 3 + 2];

            if (fnx * vnx + fny * vny + fnz * vnz < 0) {
                int tmp = b; b = c; c = tmp;
            }

            faces[fCount++] = a; faces[fCount++] = a; faces[fCount++] = a;
            faces[fCount++] = b; faces[fCount++] = b; faces[fCount++] = b;
            faces[fCount++] = c; faces[fCount++] = c; faces[fCount++] = c;
        }

        void addQuad(int a, int b, int c, int d) {
            addTriangle(a, b, c);
            addTriangle(a, c, d);
        }

        TriangleMesh build() {
            TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            mesh.getPoints().setAll(Arrays.copyOf(points, vCount * 3));
            mesh.getNormals().setAll(Arrays.copyOf(normals, vCount * 3));
            mesh.getTexCoords().setAll(Arrays.copyOf(texCoords, vCount * 2));
            mesh.getFaces().setAll(Arrays.copyOf(faces, fCount));
            return mesh;
        }
    }

    private static double[][] createRoundedRectContour(double w, double h, double r,
                                                       int cornerSteps, double barrelCurve) {
        r = Math.min(r, Math.min(w, h) * 0.48);
        int total = cornerSteps * 4;
        double[][] pts = new double[total][4];
        double hx = w * 0.5 - r;
        double hy = h * 0.5 - r;

        double[][] centers = {{hx, -hy}, {hx, hy}, {-hx, hy}, {-hx, -hy}};
        double[] startAngles = {-Math.PI * 0.5, 0.0, Math.PI * 0.5, Math.PI};

        int idx = 0;
        for (int c = 0; c < 4; c++) {
            for (int s = 0; s < cornerSteps; s++) {
                double t = (double) s / (cornerSteps - 1);
                double ang = startAngles[c] + t * (Math.PI * 0.5);
                double cos = Math.cos(ang), sin = Math.sin(ang);
                double x = centers[c][0] + r * cos;
                double y = centers[c][1] + r * sin;

                double normY = y / (h * 0.5);
                double curveScale = 1.0 + barrelCurve * (1.0 - normY * normY);
                x *= curveScale;

                pts[idx][0] = x;
                pts[idx][1] = y;
                pts[idx][2] = cos;
                pts[idx][3] = sin;
                idx++;
            }
        }
        return pts;
    }

    public static TriangleMesh createFrontHousingMesh(double w, double h) {
        MeshBuilder mb = new MeshBuilder();
        int cornerSteps = 16;
        int nPerim = cornerSteps * 4;

        double[][] profile = {
                {0.6,  4.5,  0.6,  0.8},
                {0.0,  3.8,  1.0,  0.1},
                {0.2,  0.2,  0.95, -0.3},
                {1.1, -1.5,  0.65, -0.75},
                {2.2, -2.2,  0.15, -0.98},
                {3.9, -2.2, -0.15, -0.98},
                {5.2, -0.8, -0.75, -0.65},
                {5.8,  0.0,  0.0,  -1.0}
        };

        int[][] ringIndices = new int[profile.length][nPerim];
        for (int r = 0; r < profile.length; r++) {
            double inset = profile[r][0];
            double z = profile[r][1];
            double nr = profile[r][2];
            double nz = profile[r][3];
            double[][] contour = createRoundedRectContour(
                    w - 2 * inset, h - 2 * inset, Math.max(4.0, 15.0 - inset), cornerSteps, 0.018);

            for (int i = 0; i < nPerim; i++) {
                double x = contour[i][0];
                double y = contour[i][1];
                double nx = contour[i][2] * nr;
                double ny = contour[i][3] * nr;
                double u = clamp01((x + w * 0.5) / w);
                double v = clamp01((y + h * 0.5) / h);
                ringIndices[r][i] = mb.addVertex(x, y, z, nx, ny, nz, u, v);
            }
        }

        for (int r = 0; r < profile.length - 1; r++) {
            for (int i = 0; i < nPerim; i++) {
                int next = (i + 1) % nPerim;
                mb.addQuad(ringIndices[r][i], ringIndices[r][next],
                        ringIndices[r + 1][next], ringIndices[r + 1][i]);
            }
        }

        double innerW = w - 2 * 5.6;
        double innerH = h - 2 * 5.6;
        int gx = 52, gy = 108;
        int[][] gridIdx = new int[gy + 1][gx + 1];
        double[][] gridXY = new double[(gy + 1) * (gx + 1)][2];

        for (int iy = 0; iy <= gy; iy++) {
            double ty = (double) iy / gy;
            double y = -innerH * 0.5 + ty * innerH;
            for (int ix = 0; ix <= gx; ix++) {
                double tx = (double) ix / gx;
                double x = -innerW * 0.5 + tx * innerW;

                double[] clamped = clampToRoundedRect(x, y, innerW, innerH, 9.2, 0.018);
                double cx = clamped[0], cy = clamped[1];

                double distDpad = Math.hypot(cx, cy - (-23.0));
                double z = 0.0;
                if (distDpad < 19.2) {
                    double t = 1.0 - (distDpad / 19.2);
                    z = 1.35 * t * t;
                }

                double u = clamp01((cx + w * 0.5) / w);
                double v = clamp01((cy + h * 0.5) / h);
                gridIdx[iy][ix] = mb.addVertex(cx, cy, z, 0, 0, -1, u, v);
                int flatIdx = iy * (gx + 1) + ix;
                gridXY[flatIdx][0] = cx;
                gridXY[flatIdx][1] = cy;
            }
        }

        for (int iy = 0; iy < gy; iy++) {
            for (int ix = 0; ix < gx; ix++) {
                int f00 = iy * (gx + 1) + ix;
                int f10 = (iy + 1) * (gx + 1) + ix;
                int f11 = (iy + 1) * (gx + 1) + (ix + 1);
                int f01 = iy * (gx + 1) + (ix + 1);

                double midX = 0.25 * (gridXY[f00][0] + gridXY[f10][0] + gridXY[f11][0] + gridXY[f01][0]);
                double midY = 0.25 * (gridXY[f00][1] + gridXY[f10][1] + gridXY[f11][1] + gridXY[f01][1]);

                if (isInsideLcdCutout(midX, midY) || isInsideSolarCutout(midX, midY)) {
                    continue;
                }

                mb.addQuad(gridIdx[iy][ix], gridIdx[iy + 1][ix],
                        gridIdx[iy + 1][ix + 1], gridIdx[iy][ix + 1]);
            }
        }

        return mb.build();
    }

    private static boolean isInsideLcdCutout(double x, double y) {
        return Math.abs(x - LCD_CENTER_X) < (LCD_OUTER_W * 0.5 - 3.2)
                && Math.abs(y - LCD_CENTER_Y) < (LCD_OUTER_H * 0.5 - 3.2);
    }

    private static boolean isInsideSolarCutout(double x, double y) {
        return Math.abs(x - SOLAR_CENTER_X) < (SOLAR_OUTER_W * 0.5 - 1.8)
                && Math.abs(y - SOLAR_CENTER_Y) < (SOLAR_OUTER_H * 0.5 - 1.8);
    }

    private static double[] clampToRoundedRect(double x, double y, double w, double h,
                                               double r, double barrelCurve) {
        double normY = y / (h * 0.5);
        double curveScale = 1.0 + barrelCurve * (1.0 - normY * normY);
        double rawX = x / curveScale;

        double hx = w * 0.5 - r;
        double hy = h * 0.5 - r;
        double dx = Math.max(0.0, Math.abs(rawX) - hx);
        double dy = Math.max(0.0, Math.abs(y) - hy);
        double dist = Math.hypot(dx, dy);
        if (dist > r) {
            double scale = r / dist;
            rawX = Math.signum(rawX) * (hx + dx * scale);
            y = Math.signum(y) * (hy + dy * scale);
        }
        return new double[]{rawX * curveScale, y};
    }

    public static TriangleMesh createBackHousingMesh(double w, double h, double depthZ) {
        MeshBuilder mb = new MeshBuilder();
        int cornerSteps = 16;
        int nPerim = cornerSteps * 4;

        double[][] profile = {
                {0.6,  4.5,  0.6, -0.8},
                {0.0,  5.2,  1.0, -0.1},
                {0.1,  12.5, 0.98, 0.2},
                {0.8,  16.2, 0.88, 0.48},
                {2.2,  19.0, 0.70, 0.71},
                {4.2,  20.8, 0.48, 0.88},
                {6.8,  depthZ - 0.3, 0.25, 0.97},
                {8.2,  depthZ, 0.0,  1.0}
        };

        int[][] ringIndices = new int[profile.length][nPerim];
        for (int r = 0; r < profile.length; r++) {
            double inset = profile[r][0];
            double z = profile[r][1];
            double nr = profile[r][2];
            double nz = profile[r][3];
            double[][] contour = createRoundedRectContour(
                    w - 2 * inset, h - 2 * inset, Math.max(4.0, 15.0 - inset * 0.8), cornerSteps, 0.018);

            for (int i = 0; i < nPerim; i++) {
                double x = contour[i][0];
                double y = contour[i][1];
                double nx = contour[i][2] * nr;
                double ny = contour[i][3] * nr;
                double u = clamp01((-x + w * 0.5) / w);
                double v = clamp01((y + h * 0.5) / h);
                ringIndices[r][i] = mb.addVertex(x, y, z, nx, ny, nz, u, v);
            }
        }

        for (int r = 0; r < profile.length - 1; r++) {
            for (int i = 0; i < nPerim; i++) {
                int next = (i + 1) % nPerim;
                mb.addQuad(ringIndices[r][i], ringIndices[r][next],
                        ringIndices[r + 1][next], ringIndices[r + 1][i]);
            }
        }

        int centerBack = mb.addVertex(0, 0, depthZ, 0, 0, 1, 0.5, 0.5);
        int lastRing = profile.length - 1;
        for (int i = 0; i < nPerim; i++) {
            int next = (i + 1) % nPerim;
            mb.addTriangle(centerBack, ringIndices[lastRing][i], ringIndices[lastRing][next]);
        }

        return mb.build();
    }

    public static TriangleMesh createRecessedBezelMesh(double outerW, double outerH, double outerR,
                                                       double wallWidth, double zTop, double zBottom) {
        MeshBuilder mb = new MeshBuilder();
        int cornerSteps = 12;
        int nPerim = cornerSteps * 4;

        double[][] profile = {
                {0.0,             zTop - 0.12, 0.25, -0.97},
                {0.9,             zTop - 0.22, 0.0,  -1.0},
                {1.8,             zTop - 0.05, -0.7, -0.7},
                {wallWidth - 0.4, zBottom,     -0.8, -0.6},
                {wallWidth + 0.8, zBottom + 0.12, 0.0, -1.0}
        };

        int[][] rings = new int[profile.length][nPerim];
        for (int r = 0; r < profile.length; r++) {
            double inset = profile[r][0];
            double z = profile[r][1];
            double nr = profile[r][2];
            double nz = profile[r][3];
            double[][] contour = createRoundedRectContour(
                    outerW - 2 * inset, outerH - 2 * inset, Math.max(1.2, outerR - inset * 0.65), cornerSteps, 0.0);

            for (int i = 0; i < nPerim; i++) {
                double x = contour[i][0];
                double y = contour[i][1];
                double nx = contour[i][2] * nr;
                double ny = contour[i][3] * nr;
                double u = clamp01((x + outerW * 0.5) / outerW);
                double v = clamp01((y + outerH * 0.5) / outerH);
                rings[r][i] = mb.addVertex(x, y, z, nx, ny, nz, u, v);
            }
        }

        for (int r = 0; r < profile.length - 1; r++) {
            for (int i = 0; i < nPerim; i++) {
                int next = (i + 1) % nPerim;
                mb.addQuad(rings[r][i], rings[r][next], rings[r + 1][next], rings[r + 1][i]);
            }
        }
        return mb.build();
    }

    public static TriangleMesh createRoundedPanelMesh(double w, double h, double r, double z) {
        MeshBuilder mb = new MeshBuilder();
        int cornerSteps = 10;
        double[][] contour = createRoundedRectContour(w, h, r, cornerSteps, 0.0);
        int nPerim = contour.length;

        int centerIdx = mb.addVertex(0, 0, z, 0, 0, -1, 0.5, 0.5);
        int[] ring = new int[nPerim];
        for (int i = 0; i < nPerim; i++) {
            double x = contour[i][0];
            double y = contour[i][1];
            double u = clamp01((x + w * 0.5) / w);
            double v = clamp01((y + h * 0.5) / h);
            ring[i] = mb.addVertex(x, y, z, 0, 0, -1, u, v);
        }
        for (int i = 0; i < nPerim; i++) {
            int next = (i + 1) % nPerim;
            mb.addTriangle(centerIdx, ring[i], ring[next]);
        }
        return mb.build();
    }

    public static TriangleMesh createSculptedKeyMesh(double w, double h, double r, double heightZ) {
        MeshBuilder mb = new MeshBuilder();
        int cornerSteps = 10;
        int nPerim = cornerSteps * 4;

        double[][] profile = {
                {-0.2,  0.7,               1.0,  0.0},
                {0.0,  -0.1,               0.98, -0.2},
                {0.55, -heightZ * 0.68,    0.85, -0.52},
                {1.05, -heightZ * 0.88,    0.55, -0.83},
                {1.65, -heightZ * 0.97,    0.20, -0.98},
                {2.60, -heightZ * 1.00,    0.05, -0.99}
        };

        int[][] rings = new int[profile.length][nPerim];
        for (int p = 0; p < profile.length; p++) {
            double inset = profile[p][0];
            double z = profile[p][1];
            double nr = profile[p][2];
            double nz = profile[p][3];
            double[][] contour = createRoundedRectContour(
                    w - 2 * inset, h - 2 * inset, Math.max(0.8, r - inset * 0.65), cornerSteps, 0.0);

            for (int i = 0; i < nPerim; i++) {
                double x = contour[i][0];
                double y = contour[i][1];
                double nx = contour[i][2] * nr;
                double ny = contour[i][3] * nr;
                double u = clamp01((x + w * 0.5) / w);
                double v = clamp01((y + h * 0.5) / h);
                rings[p][i] = mb.addVertex(x, y, z, nx, ny, nz, u, v);
            }
        }

        for (int p = 0; p < profile.length - 1; p++) {
            for (int i = 0; i < nPerim; i++) {
                int next = (i + 1) % nPerim;
                mb.addQuad(rings[p][i], rings[p][next], rings[p + 1][next], rings[p + 1][i]);
            }
        }

        int centerTop = mb.addVertex(0, 0, -heightZ - 0.28, 0, 0, -1, 0.5, 0.5);
        int topRing = profile.length - 1;
        for (int i = 0; i < nPerim; i++) {
            int next = (i + 1) % nPerim;
            mb.addTriangle(centerTop, rings[topRing][i], rings[topRing][next]);
        }

        return mb.build();
    }

    public static TriangleMesh createKeywellRingMesh(double keyW, double keyH, double keyR) {
        return createRecessedBezelMesh(keyW + 1.8, keyH + 1.8, keyR + 0.8, 1.3, -0.05, 0.55);
    }

    public static TriangleMesh createCircularButtonMesh(double radius, double heightZ) {
        MeshBuilder mb = new MeshBuilder();
        int segs = 40;

        double[][] profile = {
                {1.03,  0.6,            1.0,  0.0},
                {1.00, -0.1,            0.98, -0.2},
                {0.93, -heightZ * 0.72, 0.85, -0.52},
                {0.84, -heightZ * 0.92, 0.55, -0.83},
                {0.72, -heightZ * 0.99, 0.18, -0.98},
                {0.40, -heightZ * 1.03, 0.06, -0.99}
        };

        int[][] rings = new int[profile.length][segs];
        for (int p = 0; p < profile.length; p++) {
            double rad = radius * profile[p][0];
            double z = profile[p][1];
            double nr = profile[p][2];
            double nz = profile[p][3];
            for (int s = 0; s < segs; s++) {
                double ang = 2.0 * Math.PI * s / segs;
                double cos = Math.cos(ang), sin = Math.sin(ang);
                double x = rad * cos, y = rad * sin;
                double u = 0.5 + 0.48 * profile[p][0] * cos;
                double v = 0.5 + 0.48 * profile[p][0] * sin;
                rings[p][s] = mb.addVertex(x, y, z, cos * nr, sin * nr, nz, u, v);
            }
        }

        for (int p = 0; p < profile.length - 1; p++) {
            for (int s = 0; s < segs; s++) {
                int next = (s + 1) % segs;
                mb.addQuad(rings[p][s], rings[p][next], rings[p + 1][next], rings[p + 1][s]);
            }
        }

        int centerIdx = mb.addVertex(0, 0, -heightZ * 1.05, 0, 0, -1, 0.5, 0.5);
        int lastRing = profile.length - 1;
        for (int s = 0; s < segs; s++) {
            int next = (s + 1) % segs;
            mb.addTriangle(centerIdx, rings[lastRing][s], rings[lastRing][next]);
        }
        return mb.build();
    }

    public static TriangleMesh createDpadCenterHubMesh(double radius) {
        MeshBuilder mb = new MeshBuilder();
        int segs = 36;
        int center = mb.addVertex(0, 0, -1.6, 0, 0, -1, 0.5, 0.5);
        int[] ring1 = new int[segs];
        int[] ring2 = new int[segs];
        for (int s = 0; s < segs; s++) {
            double ang = 2.0 * Math.PI * s / segs;
            double cos = Math.cos(ang), sin = Math.sin(ang);
            ring1[s] = mb.addVertex(radius * 0.85 * cos, radius * 0.85 * sin, -2.0,
                    -0.2 * cos, -0.2 * sin, -0.98, 0.5 + 0.4 * cos, 0.5 + 0.4 * sin);
            ring2[s] = mb.addVertex(radius * cos, radius * sin, 0.6,
                    cos, sin, -0.2, 0.5 + 0.48 * cos, 0.5 + 0.48 * sin);
        }
        for (int s = 0; s < segs; s++) {
            int next = (s + 1) % segs;
            mb.addTriangle(center, ring1[s], ring1[next]);
            mb.addQuad(ring1[s], ring1[next], ring2[next], ring2[s]);
        }
        return mb.build();
    }

    public static TriangleMesh createDpadRockerMesh(int dir, double rInner, double rOuter) {
        MeshBuilder mb = new MeshBuilder();
        double centerAngle = switch (dir) {
            case 0 -> -Math.PI * 0.5;
            case 1 -> 0.0;
            case 2 -> Math.PI * 0.5;
            default -> Math.PI;
        };
        double halfSpan = Math.toRadians(38.0);
        int arcSteps = 18;
        int radSteps = 8;

        int[][] topGrid = new int[radSteps + 1][arcSteps + 1];
        for (int ir = 0; ir <= radSteps; ir++) {
            double tr = (double) ir / radSteps;
            double r = rInner + tr * (rOuter - rInner);
            double edgeBevelR = Math.sin(tr * Math.PI);
            double baseSlope = 1.9 + 2.3 * tr;
            for (int ia = 0; ia <= arcSteps; ia++) {
                double ta = (double) ia / arcSteps;
                double ang = centerAngle - halfSpan + ta * (2.0 * halfSpan);
                double edgeBevelA = Math.sin(ta * Math.PI);
                double bevelFactor = Math.pow(edgeBevelR * edgeBevelA, 0.28);

                double z = -(baseSlope * bevelFactor) + 0.3 * (1.0 - bevelFactor);
                double cos = Math.cos(ang), sin = Math.sin(ang);
                double x = r * cos;
                double y = r * sin;

                double nx = cos * (tr > 0.85 ? 0.55 : (tr < 0.15 ? -0.45 : -0.15));
                double ny = sin * (tr > 0.85 ? 0.55 : (tr < 0.15 ? -0.45 : -0.15));
                double nz = -0.92;

                topGrid[ir][ia] = mb.addVertex(x, y, z, nx, ny, nz, ta, 1.0 - tr);
            }
        }

        for (int ir = 0; ir < radSteps; ir++) {
            for (int ia = 0; ia < arcSteps; ia++) {
                mb.addQuad(topGrid[ir][ia], topGrid[ir + 1][ia],
                        topGrid[ir + 1][ia + 1], topGrid[ir][ia + 1]);
            }
        }

        for (int ia = 0; ia < arcSteps; ia++) {
            double ta0 = (double) ia / arcSteps;
            double ta1 = (double) (ia + 1) / arcSteps;
            double ang0 = centerAngle - halfSpan + ta0 * (2.0 * halfSpan);
            double ang1 = centerAngle - halfSpan + ta1 * (2.0 * halfSpan);

            int b0 = mb.addVertex(rOuter * Math.cos(ang0), rOuter * Math.sin(ang0), 1.1,
                    Math.cos(ang0), Math.sin(ang0), 0.0, ta0, 0.0);
            int b1 = mb.addVertex(rOuter * Math.cos(ang1), rOuter * Math.sin(ang1), 1.1,
                    Math.cos(ang1), Math.sin(ang1), 0.0, ta1, 0.0);
            mb.addQuad(topGrid[radSteps][ia], topGrid[radSteps][ia + 1], b1, b0);
        }

        return mb.build();
    }

    public static TriangleMesh createScrewHeadMesh(double radius) {
        MeshBuilder mb = new MeshBuilder();
        int segs = 24;
        int center = mb.addVertex(0, 0, 0.45, 0, 0, 1, 0.5, 0.5);
        int[] rim = new int[segs];
        for (int s = 0; s < segs; s++) {
            double ang = 2.0 * Math.PI * s / segs;
            double cos = Math.cos(ang), sin = Math.sin(ang);
            rim[s] = mb.addVertex(radius * cos, radius * sin, 0.0,
                    0.4 * cos, 0.4 * sin, 0.9, 0.5 + 0.48 * cos, 0.5 + 0.48 * sin);
        }
        for (int s = 0; s < segs; s++) {
            int next = (s + 1) % segs;
            mb.addTriangle(center, rim[s], rim[next]);
        }
        return mb.build();
    }

    public static TriangleMesh createBackRubberFootMesh(double w, double h, double r, double heightZ) {
        MeshBuilder mb = new MeshBuilder();
        int cornerSteps = 8;
        double[][] baseContour = createRoundedRectContour(w, h, r, cornerSteps, 0.0);
        double[][] topContour = createRoundedRectContour(w - 1.4, h - 1.4, Math.max(0.4, r - 0.6), cornerSteps, 0.0);
        int n = baseContour.length;

        int[] baseRing = new int[n];
        int[] topRing = new int[n];
        for (int i = 0; i < n; i++) {
            baseRing[i] = mb.addVertex(baseContour[i][0], baseContour[i][1], 0.0,
                    baseContour[i][2], baseContour[i][3], 0.3, 0.5, 0.5);
            topRing[i] = mb.addVertex(topContour[i][0], topContour[i][1], heightZ,
                    topContour[i][2] * 0.3, topContour[i][3] * 0.3, 0.95, 0.5, 0.5);
        }
        int center = mb.addVertex(0, 0, heightZ + 0.15, 0, 0, 1, 0.5, 0.5);
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            mb.addQuad(baseRing[i], baseRing[next], topRing[next], topRing[i]);
            mb.addTriangle(center, topRing[i], topRing[next]);
        }
        return mb.build();
    }

    private static double clamp01(double val) {
        return Math.max(0.0, Math.min(1.0, val));
    }
}