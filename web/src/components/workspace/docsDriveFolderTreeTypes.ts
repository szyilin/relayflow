import type { DocsDriveFolder } from '../../api/app/docs-drive'

export type DriveFolderTreeNode = DocsDriveFolder & {
  children: DriveFolderTreeNode[]
  loaded: boolean
}
