# Vectura AI — GitHub Pages branch

This branch (`pages`) holds the **static build output** that GitHub Pages
serves at <https://ahmetfurkankizil.github.io/vecturai/>.

## Do not edit files in this branch by hand

The site is a Next.js project living in a separate source repository. The
files at the root of this branch (`index.html`, `_next/`, `404.html`,
assets, etc.) are produced by `next build` with `output: 'export'` and the
contents of the resulting `out/` folder are copied here.

To publish a new version of the site:

1. In the source project, make sure `next.config.ts` has:
   ```ts
   output: "export",
   basePath: "/vecturai",
   trailingSlash: true,
   images: { unoptimized: true, /* ...remotePatterns */ },
   ```
2. Run `npm run build` to produce `out/`.
3. Replace the contents of this branch with the contents of `out/` (keep
   `LICENSE`, `.gitignore`, `.nojekyll` and this `README.md`).
4. Commit and push to `origin/pages`.

The `.nojekyll` file is required so GitHub Pages serves the `_next/`
folder verbatim instead of running it through Jekyll.
